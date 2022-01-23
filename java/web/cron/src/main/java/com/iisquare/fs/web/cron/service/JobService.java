package com.iisquare.fs.web.cron.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.PropertiesUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cron.core.WatchListener;
import com.iisquare.fs.web.cron.core.ZooKeeperClient;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobService extends ServiceBase implements DisposableBean, ApplicationListener<WebServerInitializedEvent> {

    @Value("${fs.cron.zookeeper.host}")
    private String zhHost;
    @Value("${fs.cron.zookeeper.timeout}")
    private int zhTimeout;

    private ZooKeeperClient zookeeper;
    private static volatile Scheduler scheduler = null;
    private static Logger logger = LoggerFactory.getLogger(JobService.class);

    @Override
    public void destroy() throws Exception {
        shutdown(true);
        FileUtil.close(zookeeper);
        zookeeper = null;
        scheduler = null;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String nodeAddress = InetAddress.getLoopbackAddress().getHostAddress();
        int nodePort = event.getWebServer().getPort();
        String nodeName = nodeAddress + ":" + nodePort;
        zookeeper = new ZooKeeperClient(zhHost, zhTimeout, nodeName);
        zookeeper.listen(new WatchListener(this));
        zookeeper.open();
        try {
            scheduler = scheduler();
            if (WatchListener.canStart(zookeeper.command())) {
                scheduler.start();
                zookeeper.command(WatchListener.CMD_EMPTY);
            } else {
                zookeeper.command(WatchListener.CMD_DONE_STANDBY);
            }
        } catch (Exception e) {
            logger.error("quartz service start error!", e);
            zookeeper.command(WatchListener.CMD_ERROR);
        }
    }

    public ZooKeeperClient zookeeper() {
        return this.zookeeper;
    }

    public List<String> participants() {
        return zookeeper.participants();
    }

    public Map<String, String> commands() {
        return zookeeper.commands();
    }

    public ObjectNode state() throws Exception {
        ObjectNode state = DPUtil.objectNode();
        state.put("id", zookeeper.nodeId());
        state.put("state", zookeeper.state());
        state.put("leader", zookeeper.leaderId());
        state.put("leadership", zookeeper.isLeader());
        ObjectNode scheduler = state.putObject("scheduler");
        scheduler.put("id", JobService.scheduler.getSchedulerInstanceId());
        scheduler.put("name", JobService.scheduler.getSchedulerName());
        scheduler.put("standby", JobService.scheduler.isInStandbyMode());
        scheduler.put("started", JobService.scheduler.isStarted());
        scheduler.put("shutdown", JobService.scheduler.isShutdown());
        return state;
    }

    private Scheduler scheduler() throws Exception {
        SchedulerFactory factory = new StdSchedulerFactory(
                PropertiesUtil.load(JobService.class.getClassLoader(), "quartz.properties"));
        return factory.getScheduler();
    }

    public boolean standby() {
        try {
            if (null == scheduler || scheduler.isShutdown()) return false;
            if (scheduler.isInStandbyMode() && !scheduler.isStarted()) return true;
            scheduler.standby();
            return true;
        } catch (Exception e) {
            logger.error("quartz service standby error!", e);
            return false;
        }
    }

    public boolean restart(boolean waitForJobsToComplete) {
        try {
            if (null != scheduler && scheduler.isInStandbyMode() && !scheduler.isShutdown()) {
                scheduler.start();
                return true;
            }
            shutdown(waitForJobsToComplete);
            scheduler = scheduler();
            if (null != scheduler) scheduler.start();
            return true;
        } catch (Exception e) {
            logger.error("quartz service restart error!", e);
            return false;
        }
    }

    public boolean shutdown(boolean waitForJobsToComplete) {
        if (null == scheduler) return false;
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown(waitForJobsToComplete);
            }
            return true;
        } catch (SchedulerException e) {
            logger.error("quartz service shutdown error!", e);
            return false;
        }
    }

    public Map<String, Object> standby(String nodeId) {
        return command(nodeId, WatchListener.CMD_STANDBY);
    }

    public Map<String, Object> restart(String nodeId, boolean modeForce) {
        String command = modeForce ? WatchListener.CMD_FORCE_RESTART : WatchListener.CMD_RESTART;
        return command(nodeId, command);
    }

    public Map<String, Object> shutdown(String nodeId, boolean modeForce) {
        String command = modeForce ? WatchListener.CMD_FORCE_SHUTDOWN : WatchListener.CMD_SHUTDOWN;
        return command(nodeId, command);
    }

    public Map<String, Object> command(String nodeId, String command) {
        List<String> participants = zookeeper.participants();
        if (!DPUtil.empty(nodeId)) {
            if (!participants.contains(nodeId)) return ApiUtil.result(1403, "节点异常", nodeId);
            participants = Arrays.asList(nodeId);
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String participant : participants) {
            result.put(participant, zookeeper.save("/command/" + participant, command));
        }
        return ApiUtil.result(0, null, result);
    }

}
