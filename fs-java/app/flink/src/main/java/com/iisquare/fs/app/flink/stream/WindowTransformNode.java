package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.trigger.CountTimeoutTrigger;
import com.iisquare.fs.app.flink.util.DAGElementUtil;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;

/**
 * Flink 流处理窗口节点。
 *
 * 使用项目现有的 CountTimeoutTrigger：窗口内数据达到上限或窗口时间到期时立即触发。
 * 每次触发输出一个 JSON 字符串，包含 windowStart、windowEnd、windowKey、count 和 items。
 */
public class WindowTransformNode extends AbstractConvertTransform<DataStream<String>> {

    @Override
    public DAGType<DataStream<String>> outputType() {
        return FlinkTypes.DATASTREAM_STRING;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DataStream<String> process() {
        FlinkRunner runner = runner(FlinkRunner.class);
        if (runner.isBatch()) throw new IllegalArgumentException("窗口节点仅支持Flink流处理");
        DataStream<Object> stream = FlinkUtil.union(inputs());
        if (null == stream) throw new IllegalArgumentException("窗口节点缺少上游数据");
        int seconds = options.at("/windowSeconds").asInt(0);
        if (seconds < 1) throw new IllegalArgumentException("窗口时长必须大于0秒");
        int maxCount = options.at("/maxCount").asInt(0);
        if (maxCount < 1) throw new IllegalArgumentException("窗口最大条数必须大于0");
        String keyField = options.at("/keyField").asText("").trim();
        TypeInformation<?> type = ((DataStream<?>) stream).getType();
        RowTypeInfo rowType = type instanceof RowTypeInfo ? (RowTypeInfo) type : null;
        TumblingProcessingTimeWindows assigner =
                TumblingProcessingTimeWindows.of(Duration.ofSeconds(seconds));
        if (DPUtil.empty(keyField)) {
            SingleOutputStreamOperator<String> result = stream.windowAll(assigner)
                    .trigger(new CountTimeoutTrigger<>(maxCount))
                    .process(new AllWindowJsonFunction(rowType))
                    .returns(Types.STRING);
            return result.name(getClass().getSimpleName());
        }
        SingleOutputStreamOperator<String> result = stream
                .keyBy(new FieldKeySelector(rowType, keyField))
                .window(assigner)
                .trigger(new CountTimeoutTrigger<>(maxCount))
                .process(new KeyedWindowJsonFunction(rowType))
                .returns(Types.STRING);
        return result.name(getClass().getSimpleName()).setParallelism(parallelism);
    }

    static String batch(String key, TimeWindow window, Iterable<Object> elements,
                        RowTypeInfo rowType, String timeMode) {
        ObjectNode result = DPUtil.objectNode();
        result.put("windowStart", Instant.ofEpochMilli(window.getStart()).toString());
        result.put("windowEnd", Instant.ofEpochMilli(window.getEnd()).toString());
        if (null != key) result.put("windowKey", key);
        result.put("windowSize", window.maxTimestamp() - window.getStart() + 1);
        result.put("timeMode", timeMode);
        ArrayNode items = result.putArray("items");
        int count = 0;
        for (Object element : elements) {
            items.add(DAGElementUtil.toJson(element, rowType));
            count++;
        }
        result.put("count", count);
        return DPUtil.stringify(result);
    }

    static class AllWindowJsonFunction extends ProcessAllWindowFunction<Object, String, TimeWindow> {
        private final RowTypeInfo rowType;

        AllWindowJsonFunction(RowTypeInfo rowType) {
            this.rowType = rowType;
        }

        @Override
        public void process(Context context, Iterable<Object> elements, Collector<String> out) {
            out.collect(batch(null, context.window(), elements, rowType, "processing"));
        }
    }

    static class KeyedWindowJsonFunction extends ProcessWindowFunction<Object, String, String, TimeWindow> {
        private final RowTypeInfo rowType;

        KeyedWindowJsonFunction(RowTypeInfo rowType) {
            this.rowType = rowType;
        }

        @Override
        public void process(String key, Context context, Iterable<Object> elements, Collector<String> out) {
            out.collect(batch(key, context.window(), elements, rowType, "processing"));
        }
    }

    static class FieldKeySelector implements KeySelector<Object, String> {
        private final RowTypeInfo rowType;
        private final String field;

        FieldKeySelector(RowTypeInfo rowType, String field) {
            this.rowType = rowType;
            this.field = field;
        }

        @Override
        public String getKey(Object element) {
            Object value = DAGElementUtil.fieldValue(element, rowType, field);
            return null == value ? "" : String.valueOf(value);
        }
    }
}
