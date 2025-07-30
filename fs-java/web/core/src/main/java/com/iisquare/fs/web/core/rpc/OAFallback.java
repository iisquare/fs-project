package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackFactoryBase;
import org.springframework.stereotype.Component;

@Component
public class OAFallback extends FallbackFactoryBase<OAFallback> implements OARpc {

}
