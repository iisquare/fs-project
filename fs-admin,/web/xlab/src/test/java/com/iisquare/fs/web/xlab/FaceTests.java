package com.iisquare.fs.web.xlab;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.tensorflow.FaceService;
import com.iisquare.fs.web.xlab.tensorflow.MtcnnService;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FaceTests {


    public OpenCVService cvService;
    public MtcnnService mtcnnService;
    public FaceService faceService;

    public FaceTests() {
        TensorFlowTests tensorFlowTester = new TensorFlowTests();
        cvService = tensorFlowTester.cvService;
        mtcnnService = tensorFlowTester.mtcnnService;
        faceService = tensorFlowTester.faceService;
    }

    @Test
    public void similarTest() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        System.out.println(list.subList(1, 10));
    }

    @Test
    public void videoTest() {

    }

}
