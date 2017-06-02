package com.iisquare.jwframe.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * 文件处理操作类
 */
public class FileUtil {
	
	public static boolean mkdirs(String filePath) {
		File file = new File(filePath);
		if(file.exists()) return true;
		return file.mkdirs();
	}
	
	public static boolean isExists(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}
	
	public static boolean delete(String filePath) {
		File file = new File(filePath);
		return file.delete();
	}
	
	/**
	 * 获取文件内容,默认编码
	 * @param filePath 文件路径
	 */
	public static String getContent(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) return null;
		if (!file.isFile()) return null;
		if (!file.canRead()) return null;
		Long fileLength = file.length(); // 获取文件长度
		byte[] fileContent = new byte[fileLength.intValue()];
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			inputStream.read(fileContent);
		} catch (Exception e) {
			return null;
		} finally {
			close(inputStream);
		}
		return new String(fileContent);
	}
	
	public static String getContent(String filePath, String encoding) {
		return getContent(filePath, false, encoding);
	}
	
	/**
	 * 获取文件内容
	 * @param filePath 文件路径
	 * @param bDislodgeLine 是否去除换行
	 * @param encoding 文档编码
	 * @return 文件不存在或读取异常时返回null
	 */
	public static String getContent(String filePath, boolean bDislodgeLine, String encoding) {
		if(null == encoding) return null;
		File file = new File(filePath);
		if (!file.exists()) return null;
		if (!file.isFile()) return null;
		if (!file.canRead()) return null;
		InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;
        String output = "";
		try {
			inputStream = new FileInputStream(file);
            inputReader = new InputStreamReader(inputStream, encoding);
            bufferReader = new BufferedReader(inputReader);
			StringBuilder sb = new StringBuilder();
			String text;
			while ((text = bufferReader.readLine()) != null) {
				sb.append(text);
				if(!bDislodgeLine) sb.append("\n");
			}
			int length = sb.length();
			output = length > 0 ? sb.substring(0, length - 1) : sb.toString();
		} catch (IOException ioException) {
			return null;
		} finally {
			close(bufferReader, inputReader, inputStream);
		}
		return output;
	}
	
	/**
	 * 将指定内容写入到对应文件，不存在则创建
	 * @param filePath 文件路径
	 * @param content 文件内容
	 * @param encoding 文档编码
	 * @return
	 */
	public static boolean putContent(String filePath, String content, String encoding) {
		return putContent(filePath, content, true, false, encoding);
	}
	
	/**
	 * 将指定内容写入到对应文件
	 * @param filePath 文件路径
	 * @param content 文件内容
	 * @param bCreate 不存在时是否创建
	 * @param bAppend 是否采用追加形式
	 * @param encoding 文档编码
	 * @return
	 */
	public static boolean putContent(String filePath, String content, boolean bCreate, boolean bAppend, String encoding) {
		if(null == encoding) return false;
		File file = new File(filePath);
		OutputStream outputStream = null;
        OutputStreamWriter outputWriter = null;
        BufferedWriter bufferedWriter = null;
		try {
			if (!file.exists()) {
				if(!bCreate) return false;
				if(!file.createNewFile()) return false;
			}
			if (!file.isFile()) return false;
			if (!file.canWrite()) return false;
			outputStream = new FileOutputStream(file, bAppend);
			outputWriter = new OutputStreamWriter(outputStream, encoding);
			bufferedWriter = new BufferedWriter(outputWriter);
			bufferedWriter.write(content);
			bufferedWriter.flush();
		} catch (IOException ioException) {
			return false;
		} finally {
			close(outputStream, outputWriter, bufferedWriter);
		}
		return true;
	}
	
	public static void close(Closeable...args) {
		try {
			for (Closeable arg : args) {
				if(null != arg) arg.close();
			}
		} catch (Exception e) {}
	}
}
