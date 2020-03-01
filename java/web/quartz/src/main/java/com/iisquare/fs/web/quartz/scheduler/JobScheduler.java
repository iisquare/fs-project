package com.iisquare.fs.web.quartz.scheduler;

import com.iisquare.fs.web.quartz.dao.RunLogDao;
import com.iisquare.fs.web.quartz.entity.RunLog;
import com.iisquare.fs.web.quartz.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);
    @Autowired
    private JobService jobService;
    @Autowired
    private RunLogDao runLogDao;

    @Scheduled(cron = "0 0/10 * * * ?")
    public void syncRunLog() {
        try {
            Thread.sleep(new Random().nextInt(10000));
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        List<RunLog> list = runLogDao.findByResult(JobService.RUN_LOG_RESULT_RUNNING);
        jobService.updateRunLogResult(list);
    }
}
