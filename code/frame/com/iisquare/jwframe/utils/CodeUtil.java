package com.iisquare.jwframe.utils;

import java.security.MessageDigest;

/**
 * 编码处理类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class CodeUtil {
	/**
	 * MD5加密字符串（小写）
	 */
	public static String md5(String str) {
		try {
			StringBuilder sb = new StringBuilder(32);
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] array = md.digest(str.getBytes("UTF-8"));
			for (int i = 0; i < array.length; i++) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
						.toLowerCase().substring(1, 3));
			}
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}
}
