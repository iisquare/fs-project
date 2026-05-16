package com.iisquare.fs.app.flink.trigger;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;


/**
 * 带超时的计数窗口触发器
 */
public class CountTimeoutTrigger<T> extends Trigger<T, TimeWindow> {

    /**
     * 窗口最大数据量
     */
    private final int maxCount;
    /**
     * event time / process time
     */
    private final TimeCharacteristic characteristic;
    /**
     * 用于储存窗口当前数据量的状态对象
     */
    private final ReducingStateDescriptor<Long> descriptor =
            new ReducingStateDescriptor<>("counter", new Sum(), LongSerializer.INSTANCE);

    public CountTimeoutTrigger(int maxCount) {
        this(maxCount, TimeCharacteristic.ProcessingTime);
    }

    public CountTimeoutTrigger(int maxCount, TimeCharacteristic characteristic) {
        this.maxCount = maxCount;
        this.characteristic = characteristic;
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
        if (characteristic != TimeCharacteristic.ProcessingTime) {
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
        if (characteristic != TimeCharacteristic.EventTime) {
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
