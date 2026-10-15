package com.iisquare.fs.app.flink.trigger;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;


/**
 * 带超时的计数窗口触发器。
 *
 * Flink 2.x 已移除 TimeCharacteristic，改用 eventTime 布尔标记选择生效的时间语义
 * （默认处理时间；EventTime 场景传入 true）。
 */
public class CountTimeoutTrigger<T> extends Trigger<T, TimeWindow> {

    /**
     * 窗口最大数据量
     */
    private final int maxCount;
    /**
     * 是否采用事件时间语义
     */
    private final boolean eventTime;
    /**
     * 用于储存窗口当前数据量的状态对象
     */
    private final ReducingStateDescriptor<Long> descriptor =
            new ReducingStateDescriptor<>("counter", new Sum(), LongSerializer.INSTANCE);

    public CountTimeoutTrigger(int maxCount) {
        this(maxCount, false);
    }

    public CountTimeoutTrigger(int maxCount, boolean eventTime) {
        this.maxCount = maxCount;
        this.eventTime = eventTime;
    }

    private TriggerResult fireAndPurge(TimeWindow window, TriggerContext ctx) throws Exception {
        clear(window, ctx);
        return TriggerResult.FIRE_AND_PURGE;
    }

    @Override
    public TriggerResult onElement(T element, long timestamp, TimeWindow window, TriggerContext ctx) throws Exception {
        ReducingState<Long> countState = ctx.getPartitionedState(descriptor);
        countState.add(1L);
        if (countState.get() >= maxCount) {
            return fireAndPurge(window, ctx);
        }
        if (timestamp >= window.getEnd()) {
            return fireAndPurge(window, ctx);
        } else {
            return TriggerResult.CONTINUE;
        }
    }


    @Override
    public TriggerResult onProcessingTime(long time, TimeWindow window, TriggerContext ctx) throws Exception {
        if (eventTime) {
            return TriggerResult.CONTINUE;
        }
        if (time >= window.getEnd()) {
            return TriggerResult.CONTINUE;
        } else {
            return fireAndPurge(window, ctx);
        }
    }

    @Override
    public TriggerResult onEventTime(long time, TimeWindow window, TriggerContext ctx) throws Exception {
        if (!eventTime) {
            return TriggerResult.CONTINUE;
        }
        if (time >= window.getEnd()) {
            return TriggerResult.CONTINUE;
        } else {
            return fireAndPurge(window, ctx);
        }
    }

    @Override
    public void clear(TimeWindow window, TriggerContext ctx) throws Exception {
        ReducingState<Long> countState = ctx.getPartitionedState(descriptor);
        countState.clear();
    }

    /**
     * 计数方法
     */
    static class Sum implements ReduceFunction<Long> {
        @Override
        public Long reduce(Long value1, Long value2) throws Exception {
            return value1 + value2;
        }
    }
}
