package com.iisquare.fs.ext.flink.tester;

import com.iisquare.fs.ext.flink.util.CLIUtil;
import com.iisquare.fs.ext.flink.job.CDCJob;
import org.apache.flink.client.cli.CliFrontend;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SubmitterTest {

    @Test
    public void cdcTest() {
        System.out.println("Environment FLINK_CONF_DIR=" + CLIUtil.path() + "conf");
        List<String> params = Arrays.asList(
                "run",
//                "--fromSavepoint",
//                "D:\\htdocs\\fs-project-vip\\java\\ext\\flink\\checkpoints\\7eab743ad591b2bbd3f6046384874d7a\\chk-0",
//                "--allowNonRestoredState", // 当作业发生变更时，允许保留匹配节点
                "-c",
                CDCJob.class.getName(),
                CLIUtil.path() + "build/libs/fs-project-ext-flink-0.0.1-SNAPSHOT.jar",
                "never"
        );
        CliFrontend.main(params.toArray(new String[0]));
    }

}
