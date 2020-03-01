package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/**
 * 令牌环Token
 * 采用Redis.ZSet的Score值处理任务优先级
 * Score值默认为priority，当halt停顿时长不为0时，Score值=当前时间+停顿时长
 * Score值由调度器自动设置，halt值在设置后自动归零
 */
public class Token {

    private String id; // 用于重复写入Redis
    private long halt; // 停顿间隔（毫秒时间戳），运行时变量
    private long priority; // 优先级（毫秒时间戳）
    private String groupName; // 所属分组名称
    private String scheduleId; // 所属任务ID
    private int doneCheckCount = 0; // 结束检测，运行时变量
    private long version; // 对应History的版本号，运行时变量

    public static String encode(Token token) {
        return DPUtil.stringify(DPUtil.convertJSON(token));
    }

    public static Token decode(String token) {
        return DPUtil.convertJSON(DPUtil.parseJSON(token), Token.class);
    }

    public Token active() {
        doneCheckCount = 0;
        return this;
    }

    public int done() {
        return ++doneCheckCount;
    }

    public Token halt(long halt) {
        this.halt = halt;
        return this;
    }

    public Token reuse(boolean copy) {
        if (copy) {
            Token token = new Token();
            token.id = UUID.randomUUID().toString();
            token.halt = this.halt;
            token.priority = this.priority;
            token.groupName = this.groupName;
            token.scheduleId = this.scheduleId;
            token.doneCheckCount = this.doneCheckCount;
            token.version = this.version;
            return token;
        }
        this.id = UUID.randomUUID().toString();
        return this;
    }

    public long score() {
        long priority = getPriority(), halt = getHalt();
        if (halt != 0) {
            priority = System.currentTimeMillis() + halt;
            setHalt(0); // 重置停顿间隔
        }
        return priority;
    }

    public static Token record(Schedule schedule, History history) {
        if (null == schedule) return null;
        Token token = new Token();
        token.scheduleId = schedule.getId();
        token.groupName = schedule.getGroup();
        token.priority = schedule.getPriority();
        token.version = history.getVersion();
        return token;
    }

}
