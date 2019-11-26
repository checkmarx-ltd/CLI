package com.cx.plugin.cli;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.plugin.cli.utils.CxConfigHelper;
import com.cx.plugin.cli.utils.ErrorParsingHelper;
import com.cx.restclient.CxShragaClient;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.DependencyScanResults;
import com.cx.restclient.dto.DependencyScannerType;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.dto.SASTResults;
import com.google.common.io.Files;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.impl.Log4jLoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Arrays;

import static com.cx.plugin.cli.constants.Parameters.LOG_PATH;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.INVALID_COMMAND_ERROR;

/**
 * Created by idanA on 11/4/2018.
 */

public class CxConsoleLauncher {

    private static Logger log = Logger.getLogger(CxConsoleLauncher.class);

    public static void main(String[] args) {
        int exitCode;
        Command command = null;

        try {
            if (args.length == 0) {
                throw new CLIParsingException("No arguments were given");
            }

            args = convertParamToLowerCase(args);
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(Command.getOptions(), args);
            command = Command.getCommandByValue(commandLine.getArgs()[0]);
            if (command == null) {
                throw new CLIParsingException(String.format(INVALID_COMMAND_ERROR, commandLine.getArgs()[0], Command.getAllValues()));
            }

            initLogging(commandLine);
            exitCode = execute(command, commandLine);
        } catch (CLIParsingException | ParseException e) {
            CxConfigHelper.printHelp(command);
            log.error(String.format("\n\n[CxConsole] Error parsing command: \n%s\n\n", e));
            exitCode = ErrorParsingHelper.parseError(e.getMessage());
        } catch (CxClientException | IOException | URISyntaxException | InterruptedException e) {
            log.error(String.format("%s", e.getMessage()));
            exitCode = ErrorParsingHelper.parseError(e.getMessage());
        }

        System.exit(exitCode);
    }

    private static int execute(Command command, CommandLine commandLine) throws CLIParsingException, IOException, CxClientException, URISyntaxException, InterruptedException {
        int exitCode = Errors.SCAN_SUCCEEDED.getCode();
        CxConfigHelper.printConfig(commandLine);

        CxScanConfig cxScanConfig = CxConfigHelper.resolveConfigurations(command, commandLine);

        org.slf4j.Logger logger = new Log4jLoggerFactory().getLogger(log.getName());
        CxShragaClient client = new CxShragaClient(cxScanConfig, logger);

        if (command.equals(Command.REVOKE_TOKEN)) {
            log.info(String.format("Revoking access token: %s", cxScanConfig.getRefreshToken()));
            client.revokeToken(cxScanConfig.getRefreshToken());
            return exitCode;
        }
        if (command.equals(Command.GENERATE_TOKEN)) {
            String token = client.getToken();
            log.info(String.format("The login token is: %s", token));
            return exitCode;
        }

        client.init();

        if (cxScanConfig.getSastEnabled()) {
            client.createSASTScan();
        }

        if (cxScanConfig.getDependencyScannerType() != DependencyScannerType.NONE) {
            client.createDependencyScan();
        }

        if (cxScanConfig.getSynchronous()) {
            final ScanResults scanResults = waitForResults(client, cxScanConfig);
            ScanSummary scanSummary = new ScanSummary(cxScanConfig, scanResults);
            if (scanSummary.hasErrors()) {
                log.info(scanSummary.toString());
                exitCode = ErrorParsingHelper.getErrorType(scanSummary).getCode();
            }
        }

        return exitCode;
    }

    private static ScanResults waitForResults(CxShragaClient client, CxScanConfig config) throws InterruptedException, CxClientException, IOException {
        ScanResults results = new ScanResults();

        if (config.getSastEnabled()) {
            SASTResults sastResults = client.waitForSASTResults();
            results.setSastResults(sastResults);
        }
        if (config.getDependencyScannerType() != DependencyScannerType.NONE) {
            DependencyScanResults dependencyScanResults = client.waitForDependencyScanResults();
            results.setDependencyScanResults(dependencyScanResults);
        }

        return results;
    }

    private static String[] convertParamToLowerCase(String[] args) {
        return Arrays
                .stream(args)
                .map(arg -> arg.startsWith("-") ? arg.toLowerCase() : arg)
                .toArray(String[]::new);
    }

    private static void initLogging(CommandLine commandLine) throws CLIParsingException {
        String logPath = commandLine.getOptionValue(LOG_PATH, "." + File.separator + "logs" + File.separator + "cx_console");
        File logFile = new File(logPath);
        DOMConfigurator.configure("." + File.separator + "log4j.xml");
        try {
            if (!logFile.exists()) {
                Files.createParentDirs(logFile);
                Files.touch(logFile);
            }
            Writer writer = new FileWriter(logPath);
            Appender faAppender = org.apache.log4j.Logger.getRootLogger().getAppender("FA");
            if (commandLine.hasOption(Parameters.VERBOSE)) {
                //TODO: it seems that common client overrides threshold? prints only info level
                ((RollingFileAppender) faAppender).setThreshold(Level.TRACE);
            }
            ((RollingFileAppender) faAppender).setWriter(writer);
            log.info("[CxConsole]  Log file location: " + logPath);
        } catch (IOException e) {
            throw new CLIParsingException("[CxConsole] error creating log file", e);
        }

    }
}
