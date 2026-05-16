package com.iisquare.fs.web.xlab.torch;

import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @see(https://github.com/pytorch/java-demo)
 * - 增加LIBTORCH_HOME环境变量到libtorch目录
 * - 配置LIBTORCH_HOME/lib到Path环境变量中
 */
@Service
public class TorchService extends ServiceBase {

    @Autowired
    public OpenCVService cvService;

}
