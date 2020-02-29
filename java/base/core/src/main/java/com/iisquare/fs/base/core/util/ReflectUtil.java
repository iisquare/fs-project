package com.iisquare.fs.base.core.util;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 反射处理类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ReflectUtil {

	private static Unsafe unsafe;

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (Unsafe) field.get(null);
		} catch (Exception e) {}
	}

	public static long addressOf(Object o) throws Exception {
		Object[] array = new Object[] { o };
		long baseOffset = unsafe.arrayBaseOffset(Object[].class);
		int addressSize = unsafe.addressSize();
		long objectAddress;
		switch (addressSize) {
			case 4:
				objectAddress = unsafe.getInt(array, baseOffset);
				break;
			case 8:
				objectAddress = unsafe.getLong(array, baseOffset);
				break;
			default:
				throw new Error("unsupported address size: " + addressSize);
		}
		return (objectAddress);
	}

	/**
	 * 获取某包下（包括该包的所有子包）所有类
	 * @param packageName 包名
	 * @return 类的完整名称
	 */
	public static List<String> getClassName(String packageName) {
		return getClassName(packageName, true);
	}

	/**
	 * 获取某包下所有类
	 * @param packageName 包名
	 * @param childPackage 是否遍历子包
	 * @return 类的完整名称
	 */
	public static List<String> getClassName(String packageName, boolean childPackage) {
		List<String> fileNames = null;
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		String packagePath = packageName.replace(".", "/");
		URL url = loader.getResource(packagePath);
		if (url != null) {
			String type = url.getProtocol();
			if (type.equals("file")) {
				fileNames = getClassNameByFile(url.getPath(), null, childPackage);
			} else if (type.equals("jar")) {
				fileNames = getClassNameByJar(url.getPath(), childPackage);
			}
		} else {
			fileNames = getClassNameByJars(((URLClassLoader) loader).getURLs(), packagePath, childPackage);
		}
		return fileNames;
	}

	/**
	 * 从项目文件获取某包下所有类
	 * @param filePath 文件路径
	 * @param className 类名集合
	 * @param childPackage 是否遍历子包
	 * @return 类的完整名称
	 */
	private static List<String> getClassNameByFile(String filePath, List<String> className, boolean childPackage) {
		List<String> myClassName = new ArrayList<>();
		File file = new File(filePath);
		File[] childFiles = file.listFiles();
		for (File childFile : childFiles) {
			if (childFile.isDirectory()) {
				if (childPackage) {
					myClassName.addAll(getClassNameByFile(childFile.getPath(), myClassName, childPackage));
				}
			} else {
				String childFilePath = childFile.getPath();
				if (childFilePath.endsWith(".class")) {
					childFilePath = childFilePath.substring(childFilePath.indexOf("\\classes") + 9, childFilePath.lastIndexOf("."));
					childFilePath = childFilePath.replace("\\", ".");
					myClassName.add(childFilePath);
				}
			}
		}

		return myClassName;
	}

	/**
	 * 从jar获取某包下所有类
	 * @param jarPath jar文件路径
	 * @param childPackage 是否遍历子包
	 * @return 类的完整名称
	 */
	private static List<String> getClassNameByJar(String jarPath, boolean childPackage) {
		List<String> myClassName = new ArrayList<String>();
		String[] jarInfo = jarPath.split("!");
		String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
		String packagePath = jarInfo[1].substring(1);
		try {
			JarFile jarFile = new JarFile(jarFilePath);
			Enumeration<JarEntry> entrys = jarFile.entries();
			while (entrys.hasMoreElements()) {
				JarEntry jarEntry = entrys.nextElement();
				String entryName = jarEntry.getName();
				if (entryName.endsWith(".class")) {
					if (childPackage) {
						if (entryName.startsWith(packagePath)) {
							entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
							myClassName.add(entryName);
						}
					} else {
						int index = entryName.lastIndexOf("/");
						String myPackagePath;
						if (index != -1) {
							myPackagePath = entryName.substring(0, index);
						} else {
							myPackagePath = entryName;
						}
						if (myPackagePath.equals(packagePath)) {
							entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
							myClassName.add(entryName);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return myClassName;
	}

	/**
	 * 从所有jar中搜索该包，并获取该包下所有类
	 * @param urls URL集合
	 * @param packagePath 包路径
	 * @param childPackage 是否遍历子包
	 * @return 类的完整名称
	 */
	private static List<String> getClassNameByJars(URL[] urls, String packagePath, boolean childPackage) {
		List<String> myClassName = new ArrayList<String>();
		if (urls != null) {
			for (int i = 0; i < urls.length; i++) {
				URL url = urls[i];
				String urlPath = url.getPath();
				// 不必搜索classes文件夹
				if (urlPath.endsWith("classes/")) {
					continue;
				}
				String jarPath = urlPath + "!/" + packagePath;
				myClassName.addAll(getClassNameByJar(jarPath, childPackage));
			}
		}
		return myClassName;
	}

	/**
	 * 将实体对象转化为Map
	 * @param object 对象实例
	 * @param bUnderscores 是否添加下划线
	 * @param extendFields 拓充属性数组
	 * @return
	 */
	public static Map<String, Object> convertEntityToMap(Object object, boolean bUnderscores, String[] extendFields) {
		Class<?> instance = object.getClass();
		Field[] field = instance.getDeclaredFields();
		try {
			/* 获取类属性 */
			int length = field.length;
			Object[] fields = new String[length];
			for (int i = 0; i < length; i++) {
				fields[i] = field[i].getName();
			}
			fields = DPUtil.arrayMerge(fields, extendFields); // 拓充属性数组
			/* 获取属性键值对 */
			Map<String, Object> map = new LinkedHashMap<String, Object>(DPUtil.parseInt(fields.length / 0.75f));
			for (Object item : fields) {
				String name = DPUtil.parseString(item);
				name = name.substring(0, 1).toUpperCase() + name.substring(1);
				Method method = instance.getMethod("get" + name);
				Object value = method.invoke(object);
				if(bUnderscores) name = DPUtil.addUnderscores(name);
				map.put(name, value);
			}
			return map;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 获取对象属性值
	 * @param object 对象实例
	 * @param property 属性名称
	 * @return
	 */
	public static Object getPropertyValue(Object object, String property) {
		Class<?> instance = object.getClass();
		try {
			property = property.substring(0, 1).toUpperCase() + property.substring(1);
			Method method = instance.getMethod(DPUtil.stringConcat("get", property));
			return method.invoke(object);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 设置对象属性值
	 * @param object 对象实例
	 * @param property 属性名称
	 * @param parameterTypes 参数类型，若为null值，则从属性值中自动转换类型，此时基础类型也会作为Class处理
	 * @param args 属性值
	 * @return
	 */
	public static Object setPropertyValue(Object object, String property, Class<?>[] parameterTypes, Object[] args) {
		Class<?> instance = object.getClass();
		try {
			property = property.substring(0, 1).toUpperCase() + property.substring(1);
			if(null == parameterTypes && null != args) {
				int length = args.length;
				parameterTypes = new Class<?>[length];
				for (int i = 0; i < length; i++) {
					parameterTypes[i] = args[i].getClass();
				}
			}
			Method method = instance.getMethod(DPUtil.stringConcat("set", property), parameterTypes);
			return method.invoke(object, args);
		} catch (Exception e) {
			return null;
		}
	}
}
