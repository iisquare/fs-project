package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "${rpc.member.name}", url = "${rpc.member.rest}", fallbackFactory = MemberFallback.class)
public interface MemberRpc extends RpcBase {

    @RequestMapping(value = "/rpc/email", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String email(
            @RequestPart(value = "email") String email,
            @RequestPart(value = "subject") String subject,
            @RequestPart(value = "html") String html,
            @RequestPart(value = "file") MultipartFile... files);

}
