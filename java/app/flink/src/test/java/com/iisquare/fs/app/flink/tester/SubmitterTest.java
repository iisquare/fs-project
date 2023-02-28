package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.app.flink.job.MysqlCDCJob;
import com.iisquare.fs.app.flink.util.CLIUtil;
import org.apache.flink.client.cli.CliFrontend;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 提交任务前，需先启动FlinkTester.restTest()服务
 * 确保conf配置文件的rest.port端口正确指向flink服务端口
 * 修改代码后，请重新打包，运行日志输出到rest服务的终端界面上
 * 若要终止程序执行，请在rest服务界面上进行操作，或使用cli命令手动停止
 */
public class SubmitterTest {

    @Test
    public void cdcTest() {
        System.out.println("Environment FLINK_CONF_DIR=" + CLIUtil.path() + "conf");
        List<String> params = Arrays.asList(
                "run",
                "--detached",
                "--fromSavepoint",
                "D:\\htdocs\\fs-project-vip\\java\\app\\flink\\checkpoints\\59ff4da7c238f92b38e6884cd50d330e\\chk-18",
                "--allowNonRestoredState", // 当作业发生变更时，允许保留匹配节点
                "-c",
                MysqlCDCJob.class.getName(),
                CLIUtil.path() + "build/libs/fs-project-app-flink-0.0.1-SNAPSHOT.jar",
                "never"
        );
        CliFrontend.main(params.toArray(new String[0]));
    }

}
