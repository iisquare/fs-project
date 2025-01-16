package com.iisquare.fs.web.ai.service;

import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LmChatService extends ServiceBase {

    @Value("${fs.ai.lm.chatUrl:http://127.0.0.1:7824/v1/chat/completions}")
    private String chatUrl;
    @Value("${fs.ai.lm.chatToken:fs-demo}")
    private String chatToken;
    @Value("${fs.ai.lm.chatModel:fs-chat}")
    private String chatModel;
    @Value("${fs.ai.lm.intentUrl:http://127.0.0.1:7824/v1/chat/completions}")
    private String intentUrl;
    @Value("${fs.ai.lm.intentToken:fs-demo}")
    private String intentToken;
    @Value("${fs.ai.lm.intentModel:fs-intent}")
    private String intentModel;

}
