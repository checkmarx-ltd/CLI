package com.cx.plugin.cli;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.plugin.cli.utils.CxConfigHelper;
import com.cx.plugin.cli.utils.ErrorParsingHelper;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.CxSASTClient;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.dto.SASTResults;
import com.cx.restclient.sast.utils.LegacyClient;
import com.google.common.io.Files;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.INVALID_COMMAND_COUNT;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.INVALID_COMMAND_ERROR;

/**
 * Created by idanA on 11/4/2018.
 */
public class CxConsoleLauncher {

    private static Logger log = LoggerFactory.getLogger(CxConsoleLauncher.class);

    public static void main(String[] args){
        int exitCode;
        Command command = null;

        try {
            verifyArgsCount(args);
            args = overrideProperties(args);
            args = convertParamToLowerCase(args);
            CommandLine commandLine = getCommandLine(args);
            command = getCommand(commandLine);
            initLogging(commandLine);
            exitCode = execute(command, commandLine);
        } catch (CLIParsingException | ParseException e) {
            CxConfigHelper.printHelp(command);
            log.error(String.format("\n\n[CxConsole] Error parsing command: \n%s\n\n", e));
            exitCode = ErrorParsingHelper.parseError(e.getMessage());
        } catch (CxClientException | IOException | InterruptedException e) {
        log.error(e.getMessage());
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
            throws CLIParsingException, IOException, CxClientException, InterruptedException {
        int exitCode = Errors.SCAN_SUCCEEDED.getCode();
        CxConfigHelper.printConfig(commandLine);

        CxConfigHelper configHelper = new CxConfigHelper(commandLine.getOptionValue(Parameters.CLI_CONFIG));
        CxScanConfig cxScanConfig = configHelper.resolveConfiguration(command, commandLine);

        org.slf4j.Logger logger = new Log4jLoggerFactory().getLogger(log.getName());
        CxSastConnectionProvider connectionProvider = new CxSastConnectionProvider(cxScanConfig,logger);

        CxClientDelegator clientDelegator = new CxClientDelegator(cxScanConfig, log);
        clientDelegator.init();

        if (command.equals(Command.TEST_CONNECTION)) {
            if (cxScanConfig.getAstScaConfig() != null) {
                log.info(String.format("Testing connection to: %s", cxScanConfig.getAstScaConfig().getAccessControlUrl()));
                clientDelegator.getScaClient().testScaConnection();
            } else {
                log.info(String.format("Testing connection to: %s", cxScanConfig.getUrl()));
                connectionProvider.login();
            }
            log.info("Login successful");
            return exitCode;
        }

        if (command.equals(Command.REVOKE_TOKEN)) {
            if(CxTokenExists(commandLine)){
                String token = cxScanConfig.getRefreshToken();
                token = DigestUtils.sha256Hex(token);
                log.info(String.format("Revoking access token: %s", token));
                connectionProvider.revokeToken(cxScanConfig.getRefreshToken());
                return exitCode;
            }else{
                log.error("-CxToken flag is missing.");
                exitCode=Errors.GENERAL_ERROR.getCode();
                return exitCode;
            }
        }

        if (command.equals(Command.GENERATE_TOKEN)) {
            if(UserPasswordProvided(commandLine)) {
                String token = connectionProvider.getToken();
                log.info(String.format("The login token is: %s", token));
                return exitCode;
            }else{
                log.error("-CxUser and -CxPassword flags are missing.");
                exitCode=Errors.GENERAL_ERROR.getCode();
                return exitCode;
            }
        }

        clientDelegator.initiateScan();


        if (cxScanConfig.getSynchronous()) {
            final ScanResults scanResults = clientDelegator.waitForScanResults();
            ScanSummary scanSummary = new ScanSummary(
                    cxScanConfig,
                    scanResults.getSastResults(),
                    scanResults.getOsaResults(),
                    scanResults.getScaResults()
            );

            if (scanSummary.hasErrors()) {
                log.info(scanSummary.toString());
                exitCode = ErrorParsingHelper.getErrorType(scanSummary).getCode();
            }
        }

        return exitCode;
    }

    private static CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(Command.getOptions(), args);
        return commandLine;
    }

    private static boolean CxTokenExists(CommandLine commandLine)
    {
        return commandLine.hasOption(TOKEN);
    }

    private static boolean UserPasswordProvided(CommandLine commandLine){
        return commandLine.hasOption(USER_NAME) && commandLine.hasOption(USER_PASSWORD);
    }

    private static Command getCommand(CommandLine commandLine) throws CLIParsingException {
        Command command;
        if(countCommands(commandLine)<2) {
            try {
                command = Command.getCommandByValue(commandLine.getArgs()[0]);
            } catch (Exception e) {
                throw new CLIParsingException(String.format(INVALID_COMMAND_ERROR, "", Command.getAllValues()));
            }
            if (command == null) {
                throw new CLIParsingException(String.format(INVALID_COMMAND_ERROR, commandLine.getArgs()[0], Command.getAllValues()));
            }
        }else{
            throw new CLIParsingException(String.format(INVALID_COMMAND_COUNT,commandLine.getArgList()));
        }

        return command;
    }

    private static int countCommands(CommandLine commandLine){
        int commandCount=0;
        commandCount = commandLine.getArgList().size();
        return commandCount;
    }

    private static String[] convertParamToLowerCase(String[] args) {
        return Arrays
                .stream(args)
                .map(arg -> arg.startsWith("-") ? arg.toLowerCase() : arg)
                .toArray(String[]::new);
    }

    private static void initLogging(CommandLine commandLine) throws CLIParsingException {
        String logPath = commandLine.getOptionValue(LOG_PATH, "." + File.separator + "logs" + File.separator + "cx_console.log");
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
