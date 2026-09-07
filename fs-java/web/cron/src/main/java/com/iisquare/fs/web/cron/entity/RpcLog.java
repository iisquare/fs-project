package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class RpcLog implements Serializable {

    public enum State {
        RUNNING, // 调用中
        SUCCEED, // 调用成功
        FAILED, // 调用失败
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String schedule; // Quartz实例名称
    @Column
    private String jobName;
    @Column
    private String jobGroup;
    @Column
    private String triggerName;
    @Column
    private String triggerGroup;
    @Column
    private String app; // 目标应用名称
    @Column
    private String uri; // 目标调用路径
    @Column
    private String requestHeaders; // 请求头
    @Column
    private String requestBody; // 请求体
    @Column
    private String responseHeaders; // 响应头
    @Column
    private Integer status; // 响应状态码
    @Column
    private String responseBody; // 响应体
    @Column
    private String state; // 调用状态
    @Column
    private String message; // 状态描述或异常信息
    @Column
    private Long requestTime;
    @Column
    private Long responseTime;
    @Column
    private Long duration; // 耗时毫秒

}
