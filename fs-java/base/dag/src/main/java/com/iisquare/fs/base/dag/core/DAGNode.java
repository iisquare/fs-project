package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.iisquare.fs.base.core.util.DPUtil;

/**
 * DAG 节点基类。
 *
 * 设计要点：
 * 1. 输出类型由泛型 {@code OUT} 声明，运行时通过 {@link #outputType()} 提供的
 *    {@link DAGType} 做受检转换，取代原先的 {@code Object result} + 裸强转；
 * 2. 上游取值统一走 {@link #input(int, DAGType)} / {@link #inputs(DAGType)}（推荐，可带上
 *    类型标签）或 {@link #input(Class)} / {@link #inputs(Class)}，转换只在一处，
 *    失败时抛出带节点上下文的 {@link DAGException}；
 * 3. 入边/出边使用 {@link LinkedHashSet}，保证多入度节点的上游顺序确定；
 * 4. 生命周期用 {@link State} 表达，替代两个易漏置的布尔标记。
 *
 * 注意：{@code result} 为运行期状态（可能是 DataStream/Dataset 等不可序列化对象），标记为 transient。
 */
public abstract class DAGNode<OUT> implements Serializable {

    public enum State {
        NEW, CONFIGURED, RUNNING, SUCCEEDED, FAILED
    }

    protected String id = "";
    protected String name = "";
    protected Integer parallelism = 1;
    protected JsonNode options = DPUtil.objectNode();
    private DAGRunner runner;
    @Getter
    private final Set<DAGNode<?>> sources = new LinkedHashSet<>();
    @Getter
    private final Set<DAGNode<?>> targets = new LinkedHashSet<>();
    @Getter
    private State state = State.NEW;
    private transient Object result;

    /**
     * 输出类型令牌：用于受检转换与图校验
     */
    public abstract DAGType<OUT> outputType();

    /**
     * 节点类型标识：默认为类名去掉 Node 后缀，与编排JSON中的 type 保持一致
     */
    public String getType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Node") ? simpleName.substring(0, simpleName.length() - 4) : simpleName;
    }

    /**
     * 应用上游配置节点的输出
     */
    public abstract boolean configure(JsonNode... configs);

    /**
     * 执行节点并返回输出
     */
    public abstract OUT process() throws Exception;

    /**
     * 节点输出（受检转换）
     */
    public final OUT result() {
        return outputType().cast(this, result);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }

    public void setOptions(JsonNode options) {
        this.options = null == options ? DPUtil.objectNode() : options;
    }

    public void setRunner(DAGRunner runner) {
        this.runner = runner;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getParallelism() {
        return parallelism;
    }

    public JsonNode getOptions() {
        return options;
    }

    public DAGRunner getRunner() {
        return runner;
    }

    protected final void setResult(Object value) {
        this.result = value;
    }

    protected final void setState(State state) {
        this.state = state;
    }

    public boolean isConfigured() {
        return State.NEW != state;
    }

    public boolean isProcessed() {
        return State.SUCCEEDED == state || State.FAILED == state;
    }

    /**
     * 按声明顺序返回上游节点（子流程与分组已展开）
     */
    public List<DAGNode<?>> inputs() {
        return new ArrayList<>(sources);
    }

    /**
     * 取第 index 个上游节点的输出，类型不符时抛出带上下文的异常
     */
    protected final <T> T input(int index, Class<T> type) {
        List<DAGNode<?>> inputs = inputs();
        if (index >= inputs.size()) {
            throw new DAGException(this, "缺少上游输入：index=" + index + "，实际输入数=" + inputs.size());
        }
        DAGNode<?> node = inputs.get(index);
        Object value = node.result();
        if (null == value) {
            throw new DAGException(this, "上游节点 " + node.getName() + "(" + node.getType() + ") 无输出");
        }
        if (!type.isInstance(value)) {
            throw new DAGException(this, "上游节点 " + node.getName() + "(" + node.getType() + ") 输出 "
                    + value.getClass().getName() + "，期望 " + type.getName());
        }
        return type.cast(value);
    }

    protected final <T> T input(Class<T> type) {
        return input(0, type);
    }

    /**
     * 取第 index 个上游节点的输出，使用上游声明的类型令牌做校验，
     * 类型不符时的报错会同时给出上游节点的类型标签与期望类型
     */
    protected final <T> T input(int index, DAGType<T> type) {
        List<DAGNode<?>> inputs = inputs();
        if (index >= inputs.size()) {
            throw new DAGException(this, "缺少上游输入：index=" + index + "，实际输入数=" + inputs.size());
        }
        DAGNode<?> node = inputs.get(index);
        Object value = node.result();
        if (null == value) {
            throw new DAGException(this, "上游节点 " + node.getName() + "(" + node.getType() + ") 无输出");
        }
        if (!type.isInstance(value)) {
            throw new DAGException(this, "上游节点 " + node.getName() + "(" + node.getType() + ") 输出 "
                    + node.outputType().label() + "，期望 " + type.label());
        }
        return type.cast(this, value);
    }

    protected final <T> T input(DAGType<T> type) {
        return input(0, type);
    }

    /**
     * 按顺序取全部上游输出
     */
    protected final <T> List<T> inputs(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            result.add(input(index, type));
        }
        return result;
    }

    /**
     * 按顺序取全部上游输出，使用类型令牌校验
     */
    protected final <T> List<T> inputs(DAGType<T> type) {
        List<T> result = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            result.add(input(index, type));
        }
        return result;
    }

    protected final <T> T runner(Class<T> classType) {
        if (null == runner) {
            throw new DAGException(this, "节点尚未绑定执行器");
        }
        if (!classType.isInstance(runner)) {
            throw new DAGException(this, "执行器类型不匹配：期望 " + classType.getName()
                    + "，实际 " + runner.getClass().getName());
        }
        return classType.cast(runner);
    }

}
