package com.iisquare.fs.base.core.util;

import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;

/**
 * 文件处理操作类
 */
public class FileUtil {

    public static String toBase64(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            in.read(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        } finally {
            close(in);
        }
    }

    public static File fromBase64(String base64) {
        File file;
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            file = File.createTempFile("fs-", ".base64");
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
            return file;
        } catch (Exception e) {
            return null;
        } finally {
            close(fos, bos);
        }
    }

    public static String digest(InputStream stream, int size) throws IOException {
        byte[] bytes = new byte[size];
        int length = stream.read(bytes);
        return Hex.encodeHexString(Arrays.copyOf(bytes, length));
    }

    public static boolean mkdirs(String filePath) {
        File file = new File(filePath);
        if (file.exists()) return file.isDirectory();
        return file.mkdirs();
    }

    public static boolean isExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static boolean delete(File file, boolean reduce) {
        if (!file.exists()) return true;
        if (file.isFile()) return file.delete();
        if (!reduce) return false;
        for (File item : file.listFiles()) {
            delete(item, reduce);
        }
        return file.delete();
    }

    public static boolean delete(String filePath, boolean reduce) {
        return delete(new File(filePath), reduce);
    }

    public static String getContent(String filePath) {
        return getContent(new File(filePath));
    }

    public static String getContent(File file) {
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

    public static String getContent(URL url, boolean bDislodgeLine, String encoding) {
        if (null == encoding) return null;
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;
        String output = "";
        try {
            inputStream = url.openStream();
            inputReader = new InputStreamReader(inputStream, encoding);
            bufferReader = new BufferedReader(inputReader);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = bufferReader.readLine()) != null) {
                sb.append(text);
                if (!bDislodgeLine) sb.append("\n");
            }
            output = sb.toString();
        } catch (IOException ioException) {
            return null;
        } finally {
            close(bufferReader, inputReader, inputStream);
        }
        return output;
    }

    public static String getContent(String filePath, boolean bDislodgeLine, String encoding) {
        return getContent(new File(filePath), bDislodgeLine, encoding);
    }

    public static String getContent(File file, boolean bDislodgeLine, String encoding) {
        if (null == encoding) return null;
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
                if (!bDislodgeLine) sb.append("\n");
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

    public static boolean putContent(String filePath, byte... bytes) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            FileUtil.close(fos);
        }
    }

    public static boolean putContent(String filePath, String content, String encoding) {
        return putContent(filePath, content, true, false, encoding);
    }

    public static boolean putContent(String filePath, String content, boolean bCreate, boolean bAppend, String encoding) {
        if (null == encoding) return false;
        File file = new File(filePath);
        OutputStream outputStream = null;
        OutputStreamWriter outputWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            if (!file.exists()) {
                if (!bCreate) return false;
                if (!file.createNewFile()) return false;
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

    public static void close(AutoCloseable... args) {
        try {
            for (AutoCloseable arg : args) {
                if (null != arg) arg.close();
            }
        } catch (Exception e) {
        }
    }
}
