package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * 节点输出类型令牌。
 *
 * Java 泛型在运行时会被擦除（无法书写 Class&lt;Dataset&lt;Row&gt;&gt;），因此这里用
 * “原始类型 + 可读标签”的方式描述输出类型：
 * - 运行时用 {@link #raw} 做受检转换，类型不符时抛出带节点上下文的 {@link DAGException}；
 * - {@link #label} 仅用于错误信息与执行报告展示（如 Dataset&lt;Row&gt;）。
 *
 * 注意：泛型实参无法在运行时校验（如 Dataset&lt;Row&gt; 与 Dataset&lt;String&gt; 只比较原始类型
 * Dataset），label 仅供排查问题时参考，不代表运行期约束。
 */
public final class DAGType<T> implements Serializable {

    /** 任意类型，不做限制（用于脚本节点等运行期才能确定输出类型的场景） */
    public static final DAGType<Object> OBJECT = of(Object.class, "Object");
    /** 配置节点输出：JSON */
    public static final DAGType<JsonNode> JSON = of(JsonNode.class, "JsonNode");

    private final Class<?> raw;
    private final String label;

    private DAGType(Class<?> raw, String label) {
        this.raw = raw;
        this.label = label;
    }

    /**
     * 构造类型令牌。泛型实参 T 由调用方按约定声明（与各引擎的 *Types 常量保持一致），
     * 运行期只校验 {@code raw}。
     */
    public static <T> DAGType<T> of(Class<?> raw, String label) {
        return new DAGType<>(raw, label);
    }

    public static <T> DAGType<T> of(Class<?> raw) {
        return new DAGType<>(raw, raw.getSimpleName());
    }

    public Class<?> raw() {
        return raw;
    }

    public String label() {
        return label;
    }

    public boolean isInstance(Object value) {
        return null == value || raw.isInstance(value);
    }

    /**
     * 受检转换：类型不符时抛出带节点上下文的异常，避免出现无上下文的 ClassCastException
     */
    @SuppressWarnings("unchecked")
    public T cast(DAGNode<?> node, Object value) {
        if (null == value) return null;
        if (!raw.isInstance(value)) {
            throw new DAGException(node, "输出类型不匹配：期望 " + label + "，实际 " + value.getClass().getName());
        }
        return (T) value;
    }

    @Override
    public String toString() {
        return label;
    }

}
