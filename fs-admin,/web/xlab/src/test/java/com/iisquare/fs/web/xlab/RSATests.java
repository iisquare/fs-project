package com.iisquare.fs.web.xlab;

import com.iisquare.fs.web.xlab.util.RSAUtils;
import org.junit.Test;

import java.net.URLEncoder;

public class RSATests {

    @Test
    public void utilTest() throws Exception {
        String exponent = "010001";
        String modulus = "008baf14121377fc76eaf7794b8a8af17085628c3590df47e6534574efcfd81ef8635fcdc67d141c15f51649a89533df0db839331e30b8f8e4440ebf7ccbcc494f4ba18e9f492534b8aafc1b1057429ac851d3d9eb66e86fce1b04527c7b95a2431b07ea277cde2365876e2733325df04389a9d891c5d36b7bc752140db74cb69f";
        RSAUtils rsaUtils = new RSAUtils(exponent, "", modulus);
        String password = URLEncoder.encode("Mango190803+", "UTF-8")
                .replaceAll("\\+", " ")
                .replaceAll("\\!", "!")
                .replaceAll("\\'", "'")
                .replaceAll("\\(", "(")
                .replaceAll("\\)", ")")
                .replaceAll("\\~", "~");
        String encryptedPassword = rsaUtils.encryptedString("1411093327735" + password);
        System.out.println(encryptedPassword);
    }

}
