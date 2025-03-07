package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.RSAUtil;
import org.junit.Test;

public class RSATester {

    @Test
    public void encryptionTest() throws Exception {
        ObjectNode rsa = RSAUtil.generate(1024);
        String publicKeyStr = rsa.get("publicKey").get("base64").asText();
        String privateKeyStr = rsa.get("privateKey").get("base64").asText();
        System.out.println("rsa:" +  rsa.toPrettyString());
        String data = "fs-demo";
        System.out.println("data:" + data);
        String encrypt = RSAUtil.encryptByPublicKey(data, publicKeyStr);
        System.out.println("encrypt:" + encrypt);
        String decrypt = RSAUtil.decryptByPrivateKey(encrypt, privateKeyStr);
        System.out.println("decrypt:" + decrypt);
        String sign = RSAUtil.sign(data, privateKeyStr, RSAUtil.SHA1);
        System.out.println("sign:" + sign);
        boolean verify = RSAUtil.verify(data, sign, publicKeyStr, RSAUtil.SHA1);
        System.out.println("verify:" + verify);
    }

}
