package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackFactoryBase;
import org.springframework.stereotype.Component;

@Component
public class MemberFallback extends FallbackFactoryBase<MemberFallback> implements MemberRpc {

}
