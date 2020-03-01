package com.iisquare.fs.web.admin.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Service
@FeignClient(name = "springcloud-web-quartz", fallback = CronFallback.class)
public interface CronRpc {

    @RequestMapping(method = RequestMethod.POST, value = "/job/list")
    String list(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/map")
    String map(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/add")
    String add(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/modify")
    String modify(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/pause")
    String pause(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/resume")
    String resume(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/run")
    String run(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/delete")
    String delete(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/logs")
    String logs(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/log")
    String log(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/config")
    String config(ModelMap model);

    @RequestMapping(method = RequestMethod.POST, value = "/job/serviceinfo")
    String serviceInfo();

    @RequestMapping(method = RequestMethod.POST, value = "/job/start")
    String start(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/standby")
    String standby(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/isstandby")
    String isStandby(Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/job/isvalidcron")
    String isValidCron(Map<String, Object> params);
}
