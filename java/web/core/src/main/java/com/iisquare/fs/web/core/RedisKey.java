package com.iisquare.fs.web.core;

import java.time.Duration;

public class RedisKey {

    public static Duration TTL_CRON_LOG_LOCK = Duration.ofMillis(180000);

    public static String cronLogLock() {
        return "fs:cron:lock:log";
    }

}
