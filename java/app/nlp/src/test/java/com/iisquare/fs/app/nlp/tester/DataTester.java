package com.iisquare.fs.app.nlp.tester;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.mining.word2vec.DocVectorModel;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.suggest.Suggester;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.SpeedTokenizer;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.iisquare.fs.app.nlp.bean.WordTitleNode;
import com.iisquare.fs.app.nlp.core.HanLpConfig;
import com.iisquare.fs.app.nlp.util.WordUtil;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.junit.Test;
import scala.Tuple2;

import java.io.IOException;
import java.util.List;

public class DataTester {

    @Test
    public void hlTest() throws IOException {
        String title = "#广州市财政局发布布置开展2023年度政府采购代理机构监督检查评价工作的通知#检查及评价对象";
        String content = "本次市级的监督检查及评价工作主要针对代理机构2022年执业情况，包括监督检查工作和评价工作。此次监督检查名单按照“双随机、一公开”及三年内不重复检查的原则，从代理市本级5个以上项目的40余家代理机构中，通过市市场监管局“双随机、一公开”系统以近20%的比例抽取了8家代理机构（见附件1）。鼓励名单内的被检查代理机构自愿参加评价，未在名单内、自评分在80分以上的代理机构也可申请参加评价。\n为推进各区监督检查评价能力建设，我市对各区2023年政府采购代理机构监督检查评价工作暂不作统一要求，请各区按本通知附件《监督评价项目评分明细表》和《监督评价工作依据文件清单》，参照我局组织形式自行开展监督检查评价，工作开展情况11月10日前报我局。如上级有新要求将另行通知。";
        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        List<Term> segment = analyzer.enableOffset(true).seg(content);
        WordVectorModel wordVectorModel = new WordVectorModel(HanLpConfig.MSR_VECTOR);
        DocVectorModel docVectorModel = new DocVectorModel(wordVectorModel);
        for (int i = segment.size() - 1; i >= 0; i--) {
            Term term = segment.get(i);
            float similarity = docVectorModel.similarity(term.word, title);
            if (similarity > 0.5) {
                content = content.substring(0, term.offset) + "<b>" + term.word + "</b>" + content.substring(term.offset + term.length());
            }
        }
        System.out.println(content);
    }

    @Test
    public void sparkTest() {
        Encoder<Tuple2<String, WordTitleNode>> encoder = Encoders.tuple(Encoders.STRING(), Encoders.javaSerialization(WordTitleNode.class));
        System.out.println(encoder);
    }

    @Test
    public void jiebaTest() {
        String text = "江苏省财政厅发布下达交通运输助企纾困资金的通知（苏财建〔2022〕82号）";
        JiebaSegmenter segmenter = new JiebaSegmenter();
        System.out.println(segmenter.process(text, JiebaSegmenter.SegMode.SEARCH));
    }

    @Test
    public void wordTest() {
        System.out.println(WordUtil.words("0123456789", 2, 3, 5, false));
        System.out.println(WordUtil.words("具备独立的法人资格", 4, 8, 12, false));
        System.out.println(WordUtil.words("江苏省财政厅发布下达交通运输助企纾困资金的通知（苏财建〔2022〕82号）DIP", 4, 8));
        System.out.println(WordUtil.words("河南省人力资源和社会保障厅关于印发《吉林省文化产业发展先进单位和先进个人（试行）》的通知", 4, 8));
    }

    @Test
    public void hanTest() throws IOException {
        String text = "江苏省财政厅发布下达交通运输助企纾困资金的通知（苏财建〔2022〕82号）";
        System.out.println("2. 标准分词");
        System.out.println(StandardTokenizer.segment(text));
        System.out.println("3. NLP分词");
        System.out.println(NLPTokenizer.segment(text));
        System.out.println(NLPTokenizer.analyze(text).translateLabels());
        System.out.println("4. 索引分词");
        List<Term> termList = IndexTokenizer.segment(text);
        for (Term term : termList) {
            System.out.println(term + " [" + term.offset + ":" + (term.offset + term.word.length()) + "]");
        }
        System.out.println("5. N-最短路径分词");
        Segment nShortSegment = new NShortSegment().enableAllNamedEntityRecognize(true);
        Segment shortestSegment = new DijkstraSegment().enableAllNamedEntityRecognize(true);
        System.out.println("N-最短分词：" + nShortSegment.seg(text) + "\n最短路分词：" + shortestSegment.seg(text));
        System.out.println("6. CRF分词");
        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        System.out.println(analyzer.analyze(text));
        System.out.println("7. 极速词典分词");
        System.out.println(SpeedTokenizer.segment(text));
        System.out.println("8. 用户自定义词典");
        CustomDictionary.add("助企纾困"); // 动态增加
        System.out.println(HanLP.segment(text)); // 自定义词典在所有分词器中都有效
        System.out.println("9-13. 命名实体识别");
        System.out.println(HanLP.newSegment().enableAllNamedEntityRecognize(true).seg(text));
        System.out.println("14. 关键词提取");
        System.out.println(HanLP.extractKeyword(text, 5));
        System.out.println("15. 自动摘要");
        System.out.println(HanLP.extractSummary(text, 3));
        System.out.println("16. 短语提取");
        System.out.println(HanLP.extractPhrase(text, 10));
        System.out.println("17. 拼音转换");
        System.out.println(HanLP.convertToPinyinList(text));
        System.out.println("18. 简繁转换");
        System.out.println(HanLP.convertToTraditionalChinese(text));
        System.out.println("19. 文本推荐");
        Suggester suggester = new Suggester();
        suggester.addSentence(text);
        System.out.println(suggester.suggest("通知", 1));
        System.out.println("20. 语义距离");
        WordVectorModel wordVectorModel = new WordVectorModel(HanLpConfig.MSR_VECTOR);
        System.out.println(wordVectorModel.nearest("中国"));
        System.out.println(wordVectorModel.similarity("中国", "中华"));
        DocVectorModel docVectorModel = new DocVectorModel(wordVectorModel);
        docVectorModel.addDocument(0, text);
        System.out.println(docVectorModel.nearest("困难"));
        System.out.println(docVectorModel.similarity(text, "帮助企业疏解困难"));
        System.out.println("21. 依存句法分析");
        CoNLLSentence sentence = HanLP.parseDependency(text);
        System.out.println(sentence);
    }
}
