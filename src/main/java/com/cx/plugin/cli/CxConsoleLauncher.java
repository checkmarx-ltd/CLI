package com.cx.plugin.cli;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.plugin.cli.utils.CxConfigHelper;
import com.cx.restclient.CxShragaClient;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.common.ShragaUtils;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.osa.dto.OSAResults;
import com.cx.restclient.sast.dto.SASTResults;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.impl.Log4jLoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;

import static com.cx.plugin.cli.constants.Parameters.LOG_PATH;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.*;

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
            exitCode = parseError(e);
        } catch (CxClientException | IOException | URISyntaxException | InterruptedException e) {
            log.error(String.format("%s", e.getMessage()));
            exitCode = parseError(e);
        }

        System.exit(exitCode);
    }

    private static int execute(Command command, CommandLine commandLine) throws CLIParsingException, IOException, CxClientException, URISyntaxException, InterruptedException {
        int exitCode = 0;
        printConfig(commandLine);

        ScanResults results = new ScanResults();
        CxScanConfig cxScanConfig = CxConfigHelper.resolveConfigurations(command, commandLine);

        org.slf4j.Logger logger = new Log4jLoggerFactory().getLogger(log.getName());
        CxShragaClient client = new CxShragaClient(cxScanConfig, logger);

        if (command.equals(Command.REVOKE_TOKEN)) {
            log.info(String.format("Revoking access token: [%s]", cxScanConfig.getRefreshToken()));
            client.revokeToken(cxScanConfig.getRefreshToken());
            return exitCode;
        }
        if (command.equals(Command.GENERATE_TOKEN)) {
            String token = client.getToken();
            log.info(String.format("Access token: [%s]", token));
            return exitCode;
        }

        client.init();
        if (cxScanConfig.getOsaEnabled()) {
            try {
                client.createOSAScan();
            } catch (CxClientException | IOException e) {
                results.setOsaCreateException(e);
                logger.error(e.getMessage());
            }
        }

        if (cxScanConfig.getSastEnabled()) {
            client.createSASTScan();
            if (cxScanConfig.getSynchronous()) {
                SASTResults sastResults = client.waitForSASTResults();
                OSAResults osaResults = new OSAResults();
                if (cxScanConfig.getOsaEnabled()) {
                    osaResults = client.waitForOSAResults();
                }
                results.setSastResults(sastResults);
                results.setOsaResults(osaResults);

                final String failureResult = ShragaUtils.getBuildFailureResult(cxScanConfig, sastResults, osaResults);
                if (!failureResult.isEmpty()) {
                    log.info(failureResult);
                    exitCode = resolveThresholdFailures(failureResult);
                }
            }
        }

        return exitCode;
    }

    private static int resolveThresholdFailures(String failureResult) {
        int errorCode = 0;

        if (failureResult.contains("CxSAST high")) {
            errorCode = Errors.SAST_HIGH_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST medium")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.SAST_MEDIUM_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST low")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.SAST_LOW_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA high")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_HIGH_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA medium")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_MEDIUM_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA low")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_LOW_THRESHOLD_ERROR_EXIT_CODE.getCode();
        }

        return errorCode;
    }

    //TODO: return the corresponding error code according to the error message
    private static int parseError(Exception e) {
        return 1;
    }

    //TODO: print CLI version
    private static void printConfig(CommandLine commandLine) {
        log.info("-----------------------------------------------------------------------------------------");
        log.info("CxConsole Configuration: ");
        log.info("--------------------");
        //log.info(String.format("CxConsole Version: %s", getVersion()));
        for (Option param : commandLine.getOptions()) {
            String name = param.getLongOpt() != null ? param.getLongOpt() : param.getOpt();
            String value;
            if (param.getOpt().equalsIgnoreCase(Parameters.USER_PASSWORD)) {
                value = "********";
            } else if (param.hasArg()) {
                value = param.getValue();
            } else {
                value = "true";
            }
            log.info(String.format("%s: %s", name, value));
        }
        log.info("--------------------\n\n");
    }

    private static String[] convertParamToLowerCase(String[] args) {
        return Arrays
                .stream(args)
                .map(arg -> arg.contains("-") ? arg.toLowerCase() : arg)
                .toArray(String[]::new);
    }

    private static void initLogging(CommandLine commandLine) throws CLIParsingException {
        String logPath = commandLine.getOptionValue(LOG_PATH, "./logs/cx_console");
        File logFile = new File(logPath);
        DOMConfigurator.configure("./log4j.xml");
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
