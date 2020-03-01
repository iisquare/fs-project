package com.iisquare.fs.cloud.config.controller;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Check;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.util.CronUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Api(description = "配置管理")
@RequestMapping("/")
@RestController
public class IndexController {

    @Value("${custom.config.repository}")
    private String repository;
    @Value("${custom.config.data-key}")
    private String dataKey;
    @Autowired
    private ConsulClient client;

    @ApiOperation(value = "清理离线服务")
    @GetMapping("/clear")
    public void clearAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CronUtil.initialize(response);
        CronUtil.flush(response, "getAgentChecks", true);
        Map<String, Check> checkMap = client.getAgentChecks().getValue();
        for (Map.Entry<String, Check> entry : checkMap.entrySet()) {
            Check check = entry.getValue();
            Check.CheckStatus status = check.getStatus();
            CronUtil.flush(response, check.getServiceName() + "(" + check.getServiceId() + ") " + status.name(), true);
            if(status != Check.CheckStatus.PASSING || status != Check.CheckStatus.WARNING) {
                CronUtil.flush(response, " removing...", true);
                client.agentServiceDeregister(check.getServiceId());
            }
        }
        CronUtil.flush(response, "Done", true);
    }

    @ApiOperation(value = "写配置到Consul")
    @GetMapping("/syncretize")
    public void syncretizeAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CronUtil.initialize(response);
        File repositoryDir = new File(repository);
        CronUtil.flush(response, repositoryDir.getAbsolutePath(), true);
        if(!repositoryDir.exists()) {
            CronUtil.flush(response, "文件不存在", true);
            return;
        }
        if(!repositoryDir.isDirectory()) {
            CronUtil.flush(response, "文件非目录", true);
            return;
        }
        String configRoot = "config";
        CronUtil.flush(response, "delete " + configRoot, true);
        client.deleteKVValue(configRoot);
        for (File appDir : repositoryDir.listFiles()) {
            if(!appDir.isDirectory()) continue;
            String appName = appDir.getName();
            for (File config : appDir.listFiles()) {
                if(!config.isFile()) continue;
                String dataPath = configRoot + "/" + appName;
                String filename = config.getName();
                switch (filename) {
                    case "application.yml":
                        break;
                    case "application-dev.yml":
                        dataPath += ",dev";
                        break;
                    case "application-prod.yml":
                        dataPath += ",prod";
                        break;
                    case "application-test.yml":
                        dataPath += ",test";
                        break;
                    default:
                        continue;
                }
                dataPath += "/" + dataKey;
                CronUtil.flush(response, "write " + dataPath, true);
                Response<Boolean> result = client.setKVValue(dataPath, FileUtil.getContent(config.getAbsolutePath()));
                CronUtil.flush(response, "write " + (result.getValue() ? "success" : "failed"), true);
            }
        }
        CronUtil.flush(response, "Done", true);
    }

}
