package com.iisquare.fs.web.file.service;

import com.ceph.rados.Rados;
import com.ceph.rados.exceptions.RadosException;

public class FileService {

    public void upload() throws RadosException {
        Rados rados = new Rados("admin");
        rados.confSet("mon_host", "172.16.60.41");
        rados.confSet("key", "AQCdP9pYGI4jBBAAc96J8/OconCkVKWPBNU2vg==");
    }

}
