package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.RSAUtil;
import org.junit.Test;

public class RSATester {

    @Test
    public void encryptionTest() {
        ObjectNode rsa = RSAUtil.generate(1024);
        System.out.println("rsa:" +  rsa.toPrettyString());
        String content = "fs-demo";
        System.out.println("content:" + content);
        String message = RSAUtil.encrypt(content, rsa.get("publicKey").get("base64").asText());
        System.out.println("message:" + message);
        String result = RSAUtil.decrypt(message, rsa.get("privateKey").get("base64").asText());
        System.out.println("result:" + result);
    }

}
