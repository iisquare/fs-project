package com.iisquare.fs.web.flink.cli;

import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.runtime.security.SecurityConfiguration;
import org.apache.flink.runtime.security.SecurityUtils;
import org.apache.flink.runtime.util.EnvironmentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Frontend {

    private static final Logger LOG = LoggerFactory.getLogger(Frontend.class);

    /**
     * Submits the job based on the arguments.
     */
    public static int main(final String[] args) throws Exception {
        EnvironmentInformation.logEnvironmentInfo(LOG, "Command Line Client", args);
        // 1. find the configuration directory
        final String configurationDirectory = CliFrontend.getConfigurationDirectoryFromEnv();
        // 2. load the global configuration
        final Configuration configuration = GlobalConfiguration.loadConfiguration(configurationDirectory);
        // 3. load the custom command lines
        final List<CustomCommandLine<?>> customCommandLines = CliFrontend.loadCustomCommandLines(
            configuration,
            configurationDirectory);

        final CliFrontend cli = new CliFrontend(
            configuration,
            customCommandLines);

        SecurityUtils.install(new SecurityConfiguration(cli.getConfiguration()));

        int retCode = SecurityUtils.getInstalledContext().runSecured(() -> cli.parseParameters(args));
        return retCode;
    }

}
