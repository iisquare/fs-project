package com.iisquare.fs.app.crawler.schedule;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Job {

    public final Scheduler scheduler;
    public final Token token;
    public final Schedule schedule;
    public final Task task;
    public final Template template;
    public final long time = System.currentTimeMillis();

}
