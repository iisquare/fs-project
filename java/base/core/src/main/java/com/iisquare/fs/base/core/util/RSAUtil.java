package com.iisquare.fs.base.core.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {

    /**
     * 随机生成密钥对
     * 公钥与私钥可以相互加解密，一般是：
     * 公钥进行加密，私钥进行解密，用于保护数据传输。
     * 私钥对签名（如内容的MD5值）进行加密，私钥解密出签名，用于验证数据是否被篡改。
     * RSA,RSA2公钥私钥加密解密@see(https://www.bejson.com/enc/rsa/)
     * RSA公私钥分解 Exponent（指数）、Modulus（模数）分解@see(https://www.oren.net.cn/rsa/info.html)
     */
    public static ObjectNode generate(int keySize) {
        KeyPairGenerator keyPairGen;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        // 初始化密钥对生成器
        keyPairGen.initialize(keySize, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        // 得到私钥字符串
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        // 将公钥和私钥保存到Map
        ObjectNode result = DPUtil.objectNode();
        result.put("keySize", keySize);
        result.putObject("publicKey")
                .put("base64", publicKeyString)
                .put("modulus", publicKey.getModulus())
                .put("exponent", publicKey.getPublicExponent());
        result.putObject("privateKey")
                .put("base64", privateKeyString)
                .put("modulus", privateKey.getModulus())
                .put("exponent", privateKey.getPrivateExponent());
        return result;
    }

    /**
     * RSA加密
     * 默认采用随机方式进行填充，加密结果不固定
     */
    public static String encrypt(String str, String publicKey) {
        // Base64编码的密钥
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        try {
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] bytes = cipher.doFinal(str.getBytes("UTF-8"));
            String outStr = Base64.getEncoder().encodeToString(bytes);
            return outStr;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * RSA解密
     */
    public static String decrypt(String str, String privateKey) {
        //64位解码加密后的字符串
        byte[] inputByte = Base64.getDecoder().decode(str);
        //Base64编码的密钥
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        try {
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            String outStr = new String(cipher.doFinal(inputByte));
            return outStr;
        } catch (Exception e) {
            return null;
        }
    }

    public static ObjectNode generatePublicKey(String modulus, String exponent) {
        BigInteger n = new BigInteger(modulus, 16);
        BigInteger e = new BigInteger(exponent, 16);
        RSAPublicKeyImpl publicKey;
        try {
            publicKey = new RSAPublicKeyImpl(n, e);
        } catch (InvalidKeyException ex) {
            return null;
        }
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return DPUtil.objectNode()
                .put("base64", publicKeyString)
                .put("modulus", publicKey.getModulus())
                .put("exponent", publicKey.getPublicExponent());
    }

}
