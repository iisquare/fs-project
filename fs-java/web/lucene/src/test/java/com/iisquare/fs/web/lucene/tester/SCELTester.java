package com.iisquare.fs.web.lucene.tester;

import com.iisquare.fs.web.lucene.helper.SCELHelper;
import org.junit.Test;

import java.io.File;

public class SCELTester {

    @Test
    public void sougouTest() throws Exception {
        String path = "C:\\Users\\Ouyang\\Desktop\\计算机词汇大全【官方推荐】.scel";
        SCELHelper.getInstance(new File(path)).parse().printWordList().printWordListWithPinyin();
    }

}
