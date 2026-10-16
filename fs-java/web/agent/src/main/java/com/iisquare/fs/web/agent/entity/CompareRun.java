package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 模型对比调试的运行记录：一次提交落一条，扇出到多个模型的结果整体留档。
 *
 * - 快照而非引用：settings / models / results 三列存的是「当时实际生效的」配置与输出，
 *   不去关联可变的别处配置，否则配置一改，历史记录就复现不出来了；
 * - 中断与失败同样入档：流式调到一半停止、或某个模型报错，结果照样写进来，
 *   排了半天问题却不留痕迹是最难接受的；
 * - 只存批次级信息与配置快照：settings 全局设置、models 各模型的参数；
 *   各模型的输出拆到 CompareCall 一行一次调用，这里不再重复存一份大 JSON；
 * - 大字段（列表接口不返回）：settings、models；
 * - summary / modelCount 是写入时预计算的摘要（几个完成、几个失败、几个中断），
 *   列表与历史抽屉直接展示，不必为了看一眼状态把 longtext 读出来；
 * - 删除为标记删除：deletedTime=0 表示未删除。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class CompareRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String title; // 记录标题，未填写时按用户输入截取
    @Column
    private Integer status; // 0-进行中（还有模型的流没结束），1-全部完成，2-存在失败或中断
    @Column
    private Integer modelCount; // 本轮参与的模型个数
    @Column
    private Long duration; // 全部模型结束的耗时（毫秒）
    @Column
    private String summary; // 结果摘要：几个完成、几个失败、几个中断
    @Column
    private String settings; // 全局设置快照：系统提示词、用户输入、流式与思考开关、列数
    @Column
    private String models; // 各模型的配置快照（JSON 数组）
    @Column
    private String ip; // 调用来源 IP
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long deletedTime; // 删除时间，0 表示未删除（标记删除）
    @Column
    private Integer deletedUid;

}
