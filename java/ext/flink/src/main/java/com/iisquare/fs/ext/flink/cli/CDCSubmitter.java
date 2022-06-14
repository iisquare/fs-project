package com.iisquare.fs.ext.flink.cli;

import com.iisquare.fs.ext.flink.job.CDCJob;
import com.iisquare.fs.ext.flink.util.CLIUtil;
import org.apache.flink.client.cli.CliFrontend;

import java.util.Arrays;
import java.util.List;

public class CDCSubmitter {


    public static void main(String[] args) {
        System.out.println("Edit Environment FLINK_CONF_DIR=" + CLIUtil.path() + "conf");
        List<String> params = Arrays.asList(
                "run",
                "-c",
                CDCJob.class.getName(),
                CLIUtil.path() + "build/libs/fs-project-ext-flink-0.0.1-SNAPSHOT.jar"
        );
        CliFrontend.main(params.toArray(new String[0]));
    }

}
