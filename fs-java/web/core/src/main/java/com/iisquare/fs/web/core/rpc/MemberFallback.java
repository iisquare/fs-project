package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackFactoryBase;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MemberFallback extends FallbackFactoryBase<MemberFallback> implements MemberRpc {

    @Override
    public String email(String email, String subject, String html, MultipartFile... files) {
        return fallback();
    }
}
