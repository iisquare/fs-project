package com.iisquare.fs.base.dag.util;

import org.apache.commons.cli.*;

public class CLIUtil {

    public static Options options() {
        Options options = new Options();
        options.addOption("h", "help", false, "print this usage information");
        return options;
    }

    public static CommandLine parse(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if(commandLine.hasOption("help")) {
            new HelpFormatter().printHelp("Options:", options);
            System.exit(0);
        }
        return commandLine;
    }

}
