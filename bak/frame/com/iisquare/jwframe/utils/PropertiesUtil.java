package com.iisquare.jwframe.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public class PropertiesUtil {
	public static boolean store(ClassLoader classLoader,
			Properties prop, String comments, String filePath) {
		try {
			return store(prop, comments, classLoader.getResource(filePath).toURI());
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean store(Properties prop, String comments, URI uri) {
		return store(prop, comments, new File(uri));
	}

	public static boolean store(Properties prop, String comments, String filePath) {
		return store(prop, comments, new File(filePath));
	}

	public static boolean store(Properties prop, String comments, File file) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			prop.store(fos, comments);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (null != fos) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static Properties load(ClassLoader classLoader, String filePath) {
		Properties prop = new Properties();
		try {
			InputStream in = classLoader.getResourceAsStream(filePath);
			prop.load(in);
			return prop;
		} catch (Exception e) {
			return null;
		}
	}
}
