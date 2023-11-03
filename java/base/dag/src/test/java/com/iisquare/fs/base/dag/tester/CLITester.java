package com.iisquare.fs.base.dag.tester;

import com.iisquare.fs.base.dag.util.CLIUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

public class CLITester {

    @Test
    public void argTest() throws ParseException {
        main(new String[]{
                "-t=t_demo",
                "--database=f_db"
        });
    }

    public static void main(String[] args) throws ParseException {
        CommandLine options = CLIUtil.parse(CLIUtil.options()
                .addOption("d", "database", true, "database name for read/write")
                .addOption("t", "table", true, "table name for read/write"), args);
        System.out.println(options.getOptionValue("d"));
        System.out.println(options.getOptionValue("table"));
    }
}
