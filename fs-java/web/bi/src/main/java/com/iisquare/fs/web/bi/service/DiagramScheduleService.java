package com.iisquare.fs.web.bi.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批处理定时调度服务。
 *
 * 周期扫描启用了定时调度的编排流程，按 Cron 表达式触发作业提交；同一流程存在运行中作业时跳过本次调度。
 */
@Service
public class DiagramScheduleService {

    private static final Logger log = LoggerFactory.getLogger(DiagramScheduleService.class);

    @Autowired
    DiagramDao diagramDao;
    @Autowired
    JobService jobService;
    @Autowired
    DAGService dagService;

    @Value("${fs.bi.schedule.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${fs.bi.schedule.interval:30000}", initialDelayString = "${fs.bi.schedule.initialDelay:20000}")
    public void tick() {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        List<Diagram> list = diagramDao.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("schedule"), 1),
                cb.equal(root.get("status"), 1),
                cb.isNotNull(root.get("expression"))));
        for (Diagram diagram : list) {
            try {
                fire(diagram, now);
            } catch (Exception e) {
                log.warn("[bi-diagram-schedule] 流程{}调度异常：{}", diagram.getId(), e.getMessage());
            }
        }
    }

    private void fire(Diagram diagram, long now) {
        String expression = DPUtil.trim(DPUtil.parseString(diagram.getExpression()));
        if (DPUtil.empty(expression)) return;
        CronExpression cron;
        try {
            cron = CronExpression.parse(expression);
        } catch (Exception e) {
            log.warn("[bi-diagram-schedule] 流程{}Cron表达式无效：{}", diagram.getId(), expression);
            return;
        }
        long base = null != diagram.getLastTime() && diagram.getLastTime() > 0 ? diagram.getLastTime()
                : (null != diagram.getCreatedTime() && diagram.getCreatedTime() > 0 ? diagram.getCreatedTime() : now - 1);
        ZonedDateTime next = cron.next(Instant.ofEpochMilli(base).atZone(ZoneId.systemDefault()));
        if (null == next) return;
        long nextMillis = next.toInstant().toEpochMilli();
        if (nextMillis > now) {
            if (null == diagram.getNextTime() || diagram.getNextTime() != nextMillis) {
                diagram.setNextTime(nextMillis);
                diagramDao.save(diagram);
            }
            return;
        }
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("clusterId", diagram.getClusterId());
        if (jobService.countRunning(diagram.getId()) < 1) {
            try {
                dagService.run(diagram.getId(), options, null, Job.TRIGGER_SCHEDULE);
            } catch (Exception e) {
                log.warn("[bi-diagram-schedule] 流程{}提交失败：{}", diagram.getId(), e.getMessage());
            }
        }
        diagram.setLastTime(now);
        ZonedDateTime after = cron.next(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()));
        diagram.setNextTime(null == after ? 0L : after.toInstant().toEpochMilli());
        diagramDao.save(diagram);
    }

}
