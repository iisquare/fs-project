package com.iisquare.fs.base.dag.core;

/**
 * DAG 构建与执行异常。
 *
 * 统一携带节点上下文（id/名称/类型）与执行阶段，避免出现无归属的空指针或类型转换异常。
 */
public class DAGException extends RuntimeException {

    private final String nodeId;
    private final String nodeName;
    private final String nodeType;
    private final String phase; // BUILD / CONFIGURE / RUN
    private final DAGExecution execution;
    private final String detail; // 未加前缀的原始描述，供上层包装时复用，避免前缀重复

    public DAGException(String message) {
        this((DAGNode<?>) null, "BUILD", message, null, null);
    }

    public DAGException(DAGNode<?> node, String message) {
        this(node, "RUN", message, null);
    }

    public DAGException(DAGNode<?> node, String message, Throwable cause) {
        this(node, "RUN", message, cause);
    }

    public DAGException(DAGNode<?> node, String phase, String message, Throwable cause) {
        this(node, phase, message, cause, null);
    }

    public DAGException(DAGExecution execution, DAGNode<?> node, String phase, String message, Throwable cause) {
        this(node, phase, message, cause, execution);
    }

    private DAGException(DAGNode<?> node, String phase, String message, Throwable cause, DAGExecution execution) {
        super(prefix(node, phase) + message, cause);
        this.nodeId = null == node ? null : node.getId();
        this.nodeName = null == node ? null : node.getName();
        this.nodeType = null == node ? null : node.getType();
        this.phase = phase;
        this.execution = execution;
        this.detail = message;
    }

    private static String prefix(DAGNode<?> node, String phase) {
        if (null == node) return "[" + phase + "] ";
        return "[" + phase + "] [node=" + node.getId() + "/" + node.getName() + " type=" + node.getType() + "] ";
    }

    public String nodeId() {
        return nodeId;
    }

    public String nodeName() {
        return nodeName;
    }

    public String nodeType() {
        return nodeType;
    }

    public String phase() {
        return phase;
    }

    /**
     * 未加节点前缀的原始描述
     */
    public String detail() {
        return detail;
    }

    /**
     * 节点执行到一半失败时，可通过异常拿到已执行部分的状态，便于调试与作业记录
     */
    public DAGExecution execution() {
        return execution;
    }

}
