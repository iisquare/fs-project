package com.iisquare.fs.web.cron.stage;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.cron.core.Stage;

import java.util.Map;

public class SubprocessLayoutStage extends Stage {
    @Override
    public Map<String, Object> call() throws Exception {
        return ApiUtil.result(0, null, config.toPrettyString());
    }
}
