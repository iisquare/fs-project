package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 代理资源分组。模板通过 proxyGroupId 申请该分组下的代理。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ProxyGroup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String code; // 对外接口分组标识
    @Column
    private Integer channelId;
    @Column
    private String protocol; // 为空时使用渠道协议
    @Column
    private String region; // 区域/运营商等提取参数
    @Column
    private String strategy; // round_robin、random、least_used
    @Column
    private Integer batchSize; // 单次向渠道提取数量
    @Column
    private Integer leaseSeconds; // 租约有效期
    @Column
    private Integer stickySeconds; // 状态绑定与粘性 IP 有效期
    @Column
    private Integer maxUses; // 单个租约最大使用次数
    @Column
    private Integer concurrency; // 分组最大活动租约数
    @Column
    private Integer connectTimeout;
    @Column
    private Integer socketTimeout;
    @Column
    private Integer failureThreshold; // 资源连续失败阈值
    @Column
    private String accessKey; // 外部接口访问密钥，空表示不开放外部接口
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
