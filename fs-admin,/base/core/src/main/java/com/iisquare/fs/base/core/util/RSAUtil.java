package com.iisquare.fs.base.core.util;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Cipher;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {

    // 签名算法名称
    private static final String RSA = "RSA";
    // 标准签名算法名称
    public static final String SHA1 = "SHA1withRSA";
    public static final String SHA256 = "SHA256withRSA";
    private static final Charset charset = StandardCharsets.UTF_8;

    /**
     * 生成密钥对
     * 公钥与私钥可以相互加解密，一般是：
     * 公钥对内容进行加密，私钥进行解密，用于保护数据传输。
     * 私钥对签名（如内容的MD5值）进行加密，公钥解密出签名，用于验证数据是否被篡改。
     * RSA,RSA2公钥私钥加密解密@see(https://www.bejson.com/enc/rsa/)
     * RSA公私钥分解 Exponent（指数）、Modulus（模数）分解@see(https://www.oren.net.cn/rsa/info.html)
     * @param keySize RSA密钥长度,必须是64的倍数，在512到65536位之间,推荐使用2048
     */
    public static ObjectNode generate(int keySize) throws NoSuchAlgorithmException {
        // 初始化密钥对生成器
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(keySize, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = generator.generateKeyPair();
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
     * 公钥加密(用于数据加密)
     */
    public static String encryptByPublicKey(String data, String publicKeyStr) throws Exception {
        // Base64编码的公钥
        byte[] pubKey = Base64.getDecoder().decode(publicKeyStr);
        // 创建X509编码密钥规范
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKey);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 根据X509编码密钥规范产生公钥对象
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
        // 根据转换的名称获取密码对象Cipher（转换的名称：算法/工作模式/填充模式）
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        // 用公钥初始化此Cipher对象（加密模式）
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        // 对数据加密
        byte[] encrypt = cipher.doFinal(data.getBytes(charset));
        // 返回Base64编码后的字符串
        return Base64.getEncoder().encodeToString(encrypt);
    }

    /**
     * 私钥解密(用于数据解密)
     */
    public static String decryptByPrivateKey(String data, String privateKeyStr) throws Exception {
        // Base64编码的私钥
        byte[] priKey = Base64.getDecoder().decode(privateKeyStr);
        // 创建PKCS8编码密钥规范
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(priKey);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 根据PKCS8编码密钥规范产生私钥对象
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 根据转换的名称获取密码对象Cipher（转换的名称：算法/工作模式/填充模式）
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        // 用私钥初始化此Cipher对象（解密模式）
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        // 对数据解密
        byte[] decrypt = cipher.doFinal(Base64.getDecoder().decode(data));
        // 返回字符串
        return new String(decrypt, charset);
    }

    /**
     * 私钥加密(用于数据签名)
     */
    public static String encryptByPrivateKey(String data, String privateKeyStr) throws Exception {
        // Base64编码的私钥
        byte[] priKey = Base64.getDecoder().decode(privateKeyStr);
        // 创建PKCS8编码密钥规范
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(priKey);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 根据PKCS8编码密钥规范产生私钥对象
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 根据转换的名称获取密码对象Cipher（转换的名称：算法/工作模式/填充模式）
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        // 用私钥初始化此Cipher对象（加密模式）
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        // 对数据加密
        byte[] encrypt = cipher.doFinal(data.getBytes(charset));
        // 返回Base64编码后的字符串
        return Base64.getEncoder().encodeToString(encrypt);
    }

    /**
     * 公钥解密(用于数据验签)
     */
    public static String decryptByPublicKey(String data, String publicKeyStr) throws Exception {
        // Base64编码的公钥
        byte[] pubKey = Base64.getDecoder().decode(publicKeyStr);
        // 创建X509编码密钥规范
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKey);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 根据X509编码密钥规范产生公钥对象
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
        // 根据转换的名称获取密码对象Cipher（转换的名称：算法/工作模式/填充模式）
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        // 用公钥初始化此Cipher对象（解密模式）
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        // 对数据解密
        byte[] decrypt = cipher.doFinal(Base64.getDecoder().decode(data));
        // 返回字符串
        return new String(decrypt, charset);
    }

    /**
     * RSA签名
     */
    public static String sign(String data, String privateKeyStr, String signType) throws Exception {
        // Base64编码的私钥
        byte[] priKey = Base64.getDecoder().decode(privateKeyStr);
        // 创建PKCS8编码密钥规范
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(priKey);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 根据PKCS8编码密钥规范产生私钥对象
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 标准签名算法名称(RSA还是RSA2)
        String algorithm = RSA.equals(signType) ? SHA1 : SHA256;
        // 用指定算法产生签名对象Signature
        Signature signature = Signature.getInstance(algorithm);
        // 用私钥初始化签名对象Signature
        signature.initSign(privateKey);
        // 将待签名的数据传送给签名对象(须在初始化之后)
        signature.update(data.getBytes(charset));
        // 返回签名结果字节数组
        byte[] sign = signature.sign();
        // 返回Base64编码后的字符串
        return Base64.getEncoder().encodeToString(sign);
    }

    /**
     * RSA校验数字签名
     */
    public static boolean verify(String data, String sign, String publicKeyStr, String signType) throws Exception {
        // Base64编码的公钥
        byte[] pubKey = Base64.getDecoder().decode(publicKeyStr);
        // 返回转换指定算法的KeyFactory对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        // 创建X509编码密钥规范
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKey);
        // 根据X509编码密钥规范产生公钥对象
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
        // 标准签名算法名称(RSA还是RSA2)
        String algorithm = RSA.equals(signType) ? SHA1 : SHA256;
        // 用指定算法产生签名对象Signature
        Signature signature = Signature.getInstance(algorithm);
        // 用公钥初始化签名对象,用于验证签名
        signature.initVerify(publicKey);
        // 更新签名内容
        signature.update(data.getBytes(charset));
        // 得到验证结果
        return signature.verify(Base64.getDecoder().decode(sign));
    }

}
