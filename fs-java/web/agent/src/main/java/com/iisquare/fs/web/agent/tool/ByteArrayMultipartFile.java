package com.iisquare.fs.web.agent.tool;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 内存字节数组的 MultipartFile 实现
 * 用于把文档解析出的图片字节直接投递给文件服务的上传接口
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] data;

    public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] data) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.data = null == data ? new byte[0] : data;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return data.length == 0;
    }

    @Override
    public long getSize() {
        return data.length;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        transferTo(dest.toPath());
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        Files.write(dest, data);
    }

}
