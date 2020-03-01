package com.iisquare.fs.flink.flow;

/**
 * 循环节点，每循环一次都会执行其后的全部节点
 */
@Deprecated
public interface NodeIterable<T> extends Iterable<T> {
}
