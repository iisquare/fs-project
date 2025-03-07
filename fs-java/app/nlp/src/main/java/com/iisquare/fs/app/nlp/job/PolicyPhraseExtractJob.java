package com.iisquare.fs.app.nlp.job;

import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.iisquare.fs.app.nlp.NLPCore;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import com.iisquare.fs.app.nlp.util.WordUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.MathUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class PolicyPhraseExtractJob implements Serializable {
    public static void main(String[] args) throws IOException {
        SparkConf config = ConfigUtil.spark().setAppName(PolicyPhraseExtractJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        // 获取政策标题和内容
        Dataset<Row> dataset = SourceUtil.mysql(session, NLPCore.SQL_POLICY_SAMPLE, 500);
        // 清除政策内容的HTML标签
        dataset = dataset.map((MapFunction<Row, Row>) row -> {
            String text = WordUtil.clean(row.getString(2));
            return RowFactory.create(row.get(0), row.get(1), text);
        }, dataset.encoder());
        dataset = dataset.map(new MapFunction<Row, Row>() {
            final Integer KEYWORD_MAX_COUNT = 20; // 提取关键词数量
            final Integer PHRASE_MAX_COUNT = 5; // 每个关键词下提取短语数量
            final Integer CANDIDATE_RANGE = 5; // 生成短语的候选词范围
            final List<String> STOP_PARTS = Arrays.asList( // 停用的词性列表
                    "e", // 叹词（啊）
                    "o", // 拟声词（哈哈）
                    "u", // 助词（的）
                    "w", // 标点符号
                    "m", // 数词
                    "c", // 连词
                    "q", // 量词
                    "t", // 时间词
                    "nr", // 人名
                    "ns", // 地名
                    "nt" // 机构团体
            );
            @Override
            public Row call(Row row) throws Exception {
                String title = row.getString(1);
                String content = row.getString(2);
                if (title.length() > 200) { // 处理异常标题
                    title = "";
                }
                content = content.replaceAll("[\r\n]", " ");
                content = content.replaceAll("&nbsp", " ");
                content = content.replaceAll("nbsp", " ");
                CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
                analyzer.enableAllNamedEntityRecognize(true).enableOffset(true);
                Sentence analyze = analyzer.analyze(content);
                List<Word> words = analyze.toSimpleWordList();
                Map<String, Integer> counts = new LinkedHashMap<>();
                Map<String, HashSet<Integer>> offsets = new LinkedHashMap<>();
                int size = words.size();
                for (int i = 0; i < size; i++) {
                    Word word = words.get(i);
                    if (STOP_PARTS.contains(word.getLabel())) continue;
                    String value = word.getValue();
                    offsets.computeIfAbsent(value, k -> new HashSet<>()).add(i);
                    if (value.length() < 2) continue;
                    if (value.contains(" ")) continue;
                    counts.put(value, counts.getOrDefault(value, 0) + 1);
                }
                for (Map.Entry<String, Integer> a : counts.entrySet()) { // 粗粒分词度向细粒度分词加权
                    for (Map.Entry<String, Integer> b : counts.entrySet()) {
                        if (b.getKey().contains(a.getKey())) {
                            a.setValue(a.getValue() + b.getValue());
                            offsets.get(a.getKey()).addAll(offsets.get(b.getKey()));
                        }
                    }
                }
                List<String> phrases = new LinkedList<>();
                List<String> keywords = new LinkedList<>();
                for (Map.Entry<String, Integer> entry : MathUtil.topN(counts, KEYWORD_MAX_COUNT)) {
                    String key = entry.getKey();
                    keywords.add(key);
                    Map<String, Integer> cs = new LinkedHashMap<>();
                    for (Map.Entry<String, Integer> a : counts.entrySet()) { // 将粗粒度分词加入N-Gram候选
                        if (a.getKey().contains(key)) {
                            cs.put(a.getKey(), a.getValue());
                        }
                    }
                    for (Integer offset : offsets.get(key)) {
                        for (int i = 0; i <= CANDIDATE_RANGE; i++) {
                            if (offset - i < 0) break;
                            for (int j = 0; j <= CANDIDATE_RANGE; j++) {
                                if (offset + j >= size) break;
                                WordUtil.combine(cs, STOP_PARTS, words.subList(offset - i, offset + j + 1).toArray(new Word[0]));
                            }
                        }
                    }
                    for (Map.Entry<String, Integer> ec : MathUtil.topN(cs, PHRASE_MAX_COUNT)) {
                        if ((double) ec.getValue() * 3 / entry.getValue() > 0.3) {
                            phrases.add(ec.getKey());
                        }
                    }
                }
                return RowFactory.create(
                        row.getString(0), title,
                        DPUtil.implode(",", keywords),
                        DPUtil.implode(",", WordUtil.combine(phrases)));
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("base_id", DataTypes.StringType, false),
                DataTypes.createStructField("title", DataTypes.StringType, false),
                DataTypes.createStructField("keyword", DataTypes.StringType, false),
                DataTypes.createStructField("phrase", DataTypes.StringType, false)
        ))));
        SinkUtil.mysql(dataset, SaveMode.Overwrite, "fs_policy_phrase", 200);
        session.close();
    }
}
