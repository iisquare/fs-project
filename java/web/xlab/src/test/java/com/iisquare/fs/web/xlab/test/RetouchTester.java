package com.iisquare.fs.web.xlab.test;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.RetouchService;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import org.junit.Test;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.LinkedHashMap;
import java.util.Map;

public class RetouchTester {

    @Test
    public void siteTest() {
        OpenCVService cvService = new OpenCVService();
        cvService.init("cloud-rest/xlab");
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("url", "https://cdnsfb.soufunimg.com/viewimage/1/2019_10/21/M12/45/3b0df878444c460e86e2ddf8f15c9169/690x440c.jpg");
        param.put("width", 600);
        param.put("height", 600);
        RetouchService retouchService = new RetouchService();
        retouchService.cvService = cvService;
        retouchService.watermarkService = new WatermarkService();
        Map<String, Object> result = retouchService.site(param);
        System.out.println(result);
        if (result.get("code").equals(0)) {
            System.out.println(Imgcodecs.imwrite("result.jpg",
                    cvService.imdecode(result.get("data").toString(), Imgcodecs.IMREAD_COLOR)));
        }
    }

}
