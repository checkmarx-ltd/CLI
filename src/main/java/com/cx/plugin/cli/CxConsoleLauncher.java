package com.cx.plugin.cli;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.plugin.cli.utils.CxConfigHelper;
import com.cx.plugin.cli.utils.ErrorParsingHelper;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Results;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.INVALID_COMMAND_COUNT;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.INVALID_COMMAND_ERROR;

/**
 * Created by idanA on 11/4/2018.
 */
public class CxConsoleLauncher {

    private static Logger log = LogManager.getLogger(CxConsoleLauncher.class);


    public static void main(String[] args) {
        int exitCode;
        Command command = null;
        String logLocation;
        String logLevel;

        try {
            verifyArgsCount(args);
            args = overrideProperties(args);
            args = convertParamToLowerCase(args);
            CommandLine commandLine = getCommandLine(args);
            command = getCommand(commandLine);
            logLocation = commandLine.getOptionValue(LOG_PATH, "." + File.separator + "logs" + File.separator + "cx_console.log");
            logLevel = getLogLevel(commandLine);
            initFileLogging(logLocation, logLevel);
            exitCode = execute(command, commandLine);
        } catch (CLIParsingException | ParseException | ConfigurationException e) {
            if (command == null) {
                logLocation = "." + File.separator + "logs" + File.separator + "cx_console.log";
                logLevel = "TRACE";
                initFileLogging(logLocation, logLevel);
            }
            CxConfigHelper.printHelp(command);
            log.error(String.format("%n%n[CxConsole] Error parsing command: %n%s%n%n", e));
            exitCode = ErrorParsingHelper.parseError(e.getMessage());
        } catch (CxClientException | IOException | InterruptedException e) {
            exitCode = ErrorParsingHelper.parseError(e.getMessage());
        }

        System.exit(exitCode);
    }

    private static void verifyArgsCount(String[] args) throws CLIParsingException {
        if (args.length == 0) {
            throw new CLIParsingException("No arguments were given");
        }
    }

    private static String[] overrideProperties(String[] args) {
        String propFilePath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-propFile".equals(args[i])) {
                propFilePath = args[i + 1];
                break;
            }
        }

        if (propFilePath != null) {
            try {
                log.info("Overriding properties from file: " + propFilePath);
                String argsStr = IOUtils.toString(new FileInputStream(propFilePath), Consts.UTF_8);
                args = argsStr.split("\\s+");
            } catch (Exception e) {
                log.error("can't read file", e);
            }
        }

        return args;
    }

    private static int execute(Command command, CommandLine commandLine)
            throws CLIParsingException, IOException, CxClientException, InterruptedException, ConfigurationException {
        List<ScanResults> results = new ArrayList<>();
        int exitCode = Errors.SCAN_SUCCEEDED.getCode();
        CxConfigHelper.printConfig(commandLine);
        CxConfigHelper configHelper = new CxConfigHelper(commandLine.getOptionValue(Parameters.CLI_CONFIG));
        CxScanConfig cxScanConfig = configHelper.resolveConfiguration(command, commandLine);
        org.slf4j.Logger logger = new Log4jLoggerFactory().getLogger(log.getName());

        CxSastConnectionProvider connectionProvider = new CxSastConnectionProvider(cxScanConfig, logger);

        CxClientDelegator clientDelegator = new CxClientDelegator(cxScanConfig, logger);
        ScanResults initScanResults = clientDelegator.init();
        results.add(initScanResults);

        if (command.equals(Command.TEST_CONNECTION)) {
            if (cxScanConfig.getAstScaConfig() != null) {
                String accessControlUrl = cxScanConfig.getAstScaConfig().getAccessControlUrl();
                log.info("Testing connection to: " + accessControlUrl);
                clientDelegator.getScaClient().testScaConnection();
            } else {
                String url = cxScanConfig.getUrl();
                log.info("Testing connection to: " + url);
                connectionProvider.login();
            }
            log.info("Login successful");
            return exitCode;
        }

        if (command.equals(Command.REVOKE_TOKEN)) {
            if (cxTokenExists(commandLine)) {
                String token = cxScanConfig.getRefreshToken();
                token = DigestUtils.sha256Hex(token);
                log.info("Revoking access token: " + token);
                connectionProvider.revokeToken(cxScanConfig.getRefreshToken());
                return exitCode;
            } else {
                log.error("-CxToken flag is missing.");
                exitCode = Errors.GENERAL_ERROR.getCode();
                return exitCode;
            }
        }

        if (command.equals(Command.GENERATE_TOKEN)) {
            if (userPasswordProvided(commandLine)) {
                String token = connectionProvider.getToken();
                log.info("The login token is: " + token);
                return exitCode;
            } else {
                log.error("-CxUser and -CxPassword flags are missing.");
                exitCode = Errors.GENERAL_ERROR.getCode();
                return exitCode;
            }
        }

        ScanResults createScanResults = clientDelegator.initiateScan();
        results.add(createScanResults);

        if (cxScanConfig.getSynchronous()) {
            final ScanResults scanResults = clientDelegator.waitForScanResults();
            results.add(scanResults);

            getScanResultExceptionIfExists(results);

            ScanSummary scanSummary = new ScanSummary(
                    cxScanConfig,
                    scanResults.getSastResults(),
                    scanResults.getOsaResults(),
                    scanResults.getScaResults()
            );
            String scanSummaryString = scanSummary.toString();
            if (scanSummary.hasErrors()) {
                log.info(scanSummaryString);
                exitCode = ErrorParsingHelper.getErrorType(scanSummary).getCode();
            }
        } else {
            getScanResultExceptionIfExists(results);
        }

        return exitCode;
    }

    private static void getScanResultExceptionIfExists(List<ScanResults> scanResults) {
        scanResults.forEach(scanResult -> {
            if (scanResult != null) {
                Map<ScannerType, Results> resultsMap = scanResult.getResults();
                for (Results value : resultsMap.values()) {
                    if (value != null && value.getException() != null) {
                        throw value.getException();
                    }
                }
            }
        });
    }

    private static CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(Command.getOptions(), args);
    }

    private static boolean cxTokenExists(CommandLine commandLine) {
        return commandLine.hasOption(TOKEN);
    }

    private static boolean userPasswordProvided(CommandLine commandLine) {
        return commandLine.hasOption(USER_NAME) && commandLine.hasOption(USER_PASSWORD);
    }

    private static Command getCommand(CommandLine commandLine) throws CLIParsingException {
        Command command;
        if (countCommands(commandLine) < 2) {
            try {
                command = Command.getCommandByValue(commandLine.getArgs()[0]);
            } catch (Exception e) {
                throw new CLIParsingException(String.format(INVALID_COMMAND_ERROR, "", Command.getAllValues()));
            }
            if (command == null) {
                throw new CLIParsingException(String.format(INVALID_COMMAND_ERROR, commandLine.getArgs()[0], Command.getAllValues()));
            }
        } else {
            throw new CLIParsingException(String.format(INVALID_COMMAND_COUNT, commandLine.getArgList()));
        }

        return command;
    }

    private static int countCommands(CommandLine commandLine) {
        int commandCount = 0;
        commandCount = commandLine.getArgList().size();
        return commandCount;
    }

    private static String[] convertParamToLowerCase(String[] args) {
        return Arrays
                .stream(args)
                .map(arg -> arg.startsWith("-") ? arg.toLowerCase() : arg)
                .toArray(String[]::new);
    }

    private static void initFileLogging(String logLocation, String logLevel) {
        System.setProperty("cliLogPath", logLocation);
        System.setProperty("logLevel", logLevel);
    }

    private static String getLogLevel(CommandLine commandLine) {
        return commandLine.hasOption(Parameters.VERBOSE) ? "TRACE" : "INFO";
    }
}
