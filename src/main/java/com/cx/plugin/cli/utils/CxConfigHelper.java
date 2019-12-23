package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.DependencyScannerType;
import com.cx.restclient.sca.dto.SCAConfig;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.*;

/**
 * Created by idanA on 11/5/2018.
 */
public final class CxConfigHelper {
    private static Logger log = LoggerFactory.getLogger(CxConfigHelper.class);

    private static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static PropertiesManager props = PropertiesManager.getProps();

    private Command command;
    private CommandLine commandLine;

    /**
     * Resolves configuration from the config file and the console parameters
     *
     * @param command
     * @param cmd     - The parameters passed by the user mapped by key/value
     * @return CxScanConfig an object containing all the relevant data for the scan
     */
    public CxScanConfig resolveConfiguration(Command command, CommandLine cmd) throws CLIParsingException, URISyntaxException {
        this.command = command;
        this.commandLine = cmd;

        checkForAmbiguousArgs();

        CxScanConfig scanConfig = new CxScanConfig();

        scanConfig.setSastEnabled(command.equals(Command.SCAN) || command.equals(Command.ASYNC_SCAN));

        scanConfig.setSynchronous(command.equals(Command.SCAN) ||
                command.equals(Command.OSA_SCAN) ||
                command.equals(Command.SCA_SCAN));

        scanConfig.setDependencyScannerType(getDependencyScannerType());
        scanConfig.setCxOrigin(CX_ORIGIN);

        if (scanConfig.isSastOrOSAEnabled()) {
            scanConfig.setUrl(getSastOrOsaServerUrl());
            setSastOrOsaCredentials(scanConfig);
        }

        if (command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            return scanConfig;
        }

        scanConfig.setUseSSOLogin(cmd.hasOption(IS_SSO));
        scanConfig.setDisableCertificateValidation(cmd.hasOption(TRUSTED_CERTIFICATES));

        scanConfig.setPublic(cmd.hasOption(IS_PRIVATE));
        scanConfig.setEnablePolicyViolations(cmd.hasOption(IS_CHECKED_POLICY));
        scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setTeamPath(extractTeamPath(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setPresetName(cmd.getOptionValue(PRESET) == null ? DEFAULT_PRESET_NAME : cmd.getOptionValue(PRESET));

        scanConfig.setSastFolderExclusions(getOptionalParam(LOCATION_PATH_EXCLUDE, KEY_EXCLUDED_FOLDERS));
        scanConfig.setSastFilterPattern(getOptionalParam(LOCATION_FILES_EXCLUDE, KEY_EXCLUDED_FILES));
        scanConfig.setScanComment(cmd.getOptionValue(SCAN_COMMENT));
        setScanReports(scanConfig);
        scanConfig.setIncremental(cmd.hasOption(IS_INCREMENTAL));
        scanConfig.setForceScan(!cmd.hasOption(IS_FORCE_SCAN));
        setSASTThresholds(scanConfig);

        String dsLocationPath = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        if (dsLocationPath == null) {
            ScanSourceConfigurator locator = new ScanSourceConfigurator();
            locator.configureSourceLocation(commandLine, props, scanConfig);
        }

        scanConfig.setProgressInterval(props.getIntProperty(KEY_PROGRESS_INTERVAL));
        scanConfig.setConnectionRetries(props.getIntProperty(KEY_RETRIES));
        scanConfig.setDefaultProjectName(props.getProperty(KEY_DEF_PROJECT_NAME));
        
        configureDependencyScan(scanConfig);

        return scanConfig;
    }

    private void configureDependencyScan(CxScanConfig scanConfig) throws CLIParsingException {
        if (scanConfig.getDependencyScannerType() == DependencyScannerType.NONE) {
            return;
        }

        if (scanConfig.getDependencyScannerType() == DependencyScannerType.OSA) {
            setOsaSpecificConfig(scanConfig);
        } else {
            setScaSpecificConfig(scanConfig);
        }

        setSharedDependencyScanConfig(scanConfig);
    }

    private void setOsaSpecificConfig(CxScanConfig scanConfig) {
        scanConfig.setOsaRunInstall(commandLine.hasOption(INSTALL_PACKAGE_MANAGER));

        String reportDir = commandLine.getOptionValue(OSA_JSON_REPORT);
        scanConfig.setReportsDir(reportDir != null ? new File(reportDir) : null);
        scanConfig.setOsaGenerateJsonReport(reportDir != null);

        String archiveIncludePatterns = getOptionalParam(OSA_ARCHIVE_TO_EXTRACT, KEY_OSA_EXTRACTABLE_INCLUDE_FILES);
        scanConfig.setOsaArchiveIncludePatterns(archiveIncludePatterns);

        String osaScanDepth = getOptionalParam(OSA_SCAN_DEPTH, KEY_OSA_SCAN_DEPTH);
        scanConfig.setOsaScanDepth(osaScanDepth);
    }

    private void setScaSpecificConfig(CxScanConfig scanConfig) throws CLIParsingException {
        SCAConfig sca = new SCAConfig();

        String apiUrl = normalizeUrl(getRequiredParam(commandLine, SCA_API_URL, KEY_SCA_API_URL));
        sca.setApiUrl(apiUrl);

        String acUrl = normalizeUrl(getRequiredParam(commandLine, SCA_ACCESS_CONTROL_URL, KEY_SCA_ACCESS_CONTROL_URL));
        sca.setAccessControlUrl(acUrl);

        String webAppUrl = normalizeUrl(getRequiredParam(commandLine, SCA_WEB_APP_URL, KEY_SCA_WEB_APP_URL));
        sca.setWebAppUrl(webAppUrl);

        sca.setUsername(getRequiredParam(commandLine, SCA_USERNAME, null));
        sca.setPassword(getRequiredParam(commandLine, SCA_PASSWORD, null));
        sca.setTenant(getRequiredParam(commandLine, SCA_TENANT, null));

        scanConfig.setScaConfig(sca);
    }

    private void setSharedDependencyScanConfig(CxScanConfig scanConfig) {
        setDependencyScanThresholds(scanConfig);

        String location = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        scanConfig.setOsaLocationPath(location);

        String folderExclusions = getSharedDependencyScanOption(scanConfig, OSA_FOLDER_EXCLUDE, SCA_FOLDER_EXCLUDE);
        scanConfig.setOsaFolderExclusions(folderExclusions);

        String key = scanConfig.getDependencyScannerType() == DependencyScannerType.OSA ?
                KEY_OSA_PROGRESS_INTERVAL :
                KEY_SCA_PROGRESS_INTERVAL;
        scanConfig.setOsaProgressInterval(props.getIntProperty(key));

        setDependencyScanFilterPattern(scanConfig);
    }

    private void setDependencyScanFilterPattern(CxScanConfig scanConfig) {
        String includedFiles, excludedFiles;
        if (scanConfig.getDependencyScannerType() == DependencyScannerType.OSA) {
            includedFiles = getOptionalParam(OSA_FILES_INCLUDE, KEY_OSA_INCLUDED_FILES);
            excludedFiles = getOptionalParam(OSA_FILES_EXCLUDE, KEY_OSA_EXCLUDED_FILES);
        } else {
            includedFiles = getOptionalParam(SCA_FILES_INCLUDE, KEY_SCA_INCLUDED_FILES);
            excludedFiles = getOptionalParam(SCA_FILES_EXCLUDE, KEY_SCA_EXCLUDED_FILES);
        }

        String filterPattern = null;
        if (includedFiles != null) {
            if (excludedFiles != null) {
                filterPattern = includedFiles + ", " + excludedFiles;
            } else
                filterPattern = includedFiles;
        } else if (excludedFiles != null) {
            filterPattern = excludedFiles;
        }

        scanConfig.setOsaFilterPattern(filterPattern);
    }

    private void setSastOrOsaCredentials(CxScanConfig scanConfig) throws CLIParsingException {
        boolean isSSO = commandLine.hasOption(IS_SSO);
        String token = commandLine.getOptionValue(TOKEN);
        String username = commandLine.getOptionValue(USER_NAME);
        String password = commandLine.getOptionValue(USER_PASSWORD);
        if (StringUtils.isNotEmpty(token)) {
            scanConfig.setRefreshToken(token);
        } else if ((Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) && !isSSO) {
            throw new CLIParsingException("[CxConsole] User name and password are mandatory unless SSO or token is used");
        } else {
            scanConfig.setUsername(username);
            scanConfig.setPassword(password);
        }
    }

    private String getSastOrOsaServerUrl() throws CLIParsingException {
        String rawUrl = getRequiredParam(commandLine, SERVER_URL, null);
        return normalizeUrl(rawUrl);
    }

    private void checkForAmbiguousArgs() throws CLIParsingException {
        String message = null;
        if (commandLine.hasOption(OSA_ENABLED) && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s and %s arguments cannot be specified together.", OSA_ENABLED, SCA_ENABLED);
        }
        else if (command == Command.SCA_SCAN && commandLine.hasOption(OSA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", OSA_ENABLED, command);
        }
        else if (command == Command.OSA_SCAN && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", OSA_ENABLED, command);
        }

        if (message != null) {
            throw new CLIParsingException(message);
        }
    }

    private void setScanReports(CxScanConfig scanConfig) {
        String reportPath = getReportPath(PDF_REPORT);
        if (reportPath != null) {
            scanConfig.addPDFReport(reportPath);
        }

        reportPath = getReportPath(XML_REPORT);
        if (reportPath != null) {
            scanConfig.addXMLReport(reportPath);
        }

        reportPath = getReportPath(CSV_REPORT);
        if (reportPath != null) {
            scanConfig.addCSVReport(reportPath);
        }

        reportPath = getReportPath(RTF_REPORT);
        if (reportPath != null) {
            scanConfig.addRTFReport(reportPath);
        }
    }

    private String getReportPath(String optionName) {
        String rawValue = commandLine.getOptionValue(optionName);
        String result = null;
        if (rawValue != null) {
            result = rawValue.replace("..\\", "")
                    .replace("..//", "");
        }
        return result;
    }

    private CxScanConfig setSASTThresholds(CxScanConfig scanConfig) {
        String sastHigh = commandLine.getOptionValue(SAST_HIGH);
        String sastMedium = commandLine.getOptionValue(SAST_MEDIUM);
        String sastLow = commandLine.getOptionValue(SAST_LOW);

        scanConfig.setSastThresholdsEnabled(false);
        if (!Strings.isNullOrEmpty(sastHigh)) {
            scanConfig.setSastHighThreshold(Integer.valueOf(sastHigh));
            scanConfig.setSastThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(sastMedium)) {
            scanConfig.setSastMediumThreshold(Integer.valueOf(sastMedium));
            scanConfig.setSastThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(sastLow)) {
            scanConfig.setSastLowThreshold(Integer.valueOf(sastLow));
            scanConfig.setSastThresholdsEnabled(true);
        }

        return scanConfig;
    }

    private void setDependencyScanThresholds(CxScanConfig scanConfig) {
        String high = getSharedDependencyScanOption(scanConfig, OSA_HIGH, SCA_HIGH);
        String medium = getSharedDependencyScanOption(scanConfig, OSA_MEDIUM, SCA_MEDIUM);
        String low = getSharedDependencyScanOption(scanConfig, OSA_LOW, SCA_LOW);

        scanConfig.setOsaThresholdsEnabled(false);
        if (!Strings.isNullOrEmpty(high)) {
            scanConfig.setOsaHighThreshold(Integer.valueOf(high));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(medium)) {
            scanConfig.setOsaMediumThreshold(Integer.valueOf(medium));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(low)) {
            scanConfig.setOsaLowThreshold(Integer.valueOf(low));
            scanConfig.setOsaThresholdsEnabled(true);
        }
    }

    /**
     * Get one of the command line options that are shared between OSA and SCA and differ only
     * in prefix ("osa" or "sca").
     */
    private String getSharedDependencyScanOption(CxScanConfig scanConfig, String osaOption, String scaOption) {
        String result = null;
        if (scanConfig.getDependencyScannerType() == DependencyScannerType.OSA) {
            result = commandLine.getOptionValue(osaOption);
        } else if (scanConfig.getDependencyScannerType() == DependencyScannerType.SCA) {
            result = commandLine.getOptionValue(scaOption);
        }
        return result;
    }

    private DependencyScannerType getDependencyScannerType() {
        DependencyScannerType result;
        if (command == Command.OSA_SCAN || command == Command.ASYNC_OSA_SCAN || commandLine.hasOption(OSA_ENABLED)) {
            result = DependencyScannerType.OSA;
        } else if (command == Command.SCA_SCAN || command == Command.ASYNC_SCA_SCAN || commandLine.hasOption(SCA_ENABLED)) {
            result = DependencyScannerType.SCA;
        }
        else {
            result = DependencyScannerType.NONE;
        }
        return result;
    }

    private static int getLastIndexOfTeam(String fullPath) {
        int lastIdx = fullPath.lastIndexOf("\\");
        return lastIdx != -1 ? lastIdx : fullPath.lastIndexOf("/");
    }

    private static String extractProjectName(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath);
        return fullPath.substring(lastIdx + 1);
    }

    private static String extractTeamPath(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath);
        return fullPath.substring(0, lastIdx);
    }

    public static void printConfig(CommandLine commandLine) {
        log.info("-----------------------------------------------------------------------------------------");
        log.info("CxConsole Configuration: ");
        log.info("--------------------");
        //log.info(String.format("CxConsole Version: %s", getVersion()));
        for (Option param : commandLine.getOptions()) {
            String name = param.getLongOpt() != null ? param.getLongOpt() : param.getOpt();
            String value;
            if (param.getOpt().equalsIgnoreCase(Parameters.USER_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(SCA_PASSWORD)) {
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

    public static void printHelp(Command mode) {
        if (mode == null) {
            return;
        }

        String footerFormat = "\nUsage example: %s\n\n(c) 2019 CheckMarx.com LTD, All Rights Reserved\n";
        String helpHeader = mode.getDescription();
        String helpFooter = String.format(footerFormat, mode.getUsageExamples());

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(120, mode.value(), helpHeader, mode.getOptions(), helpFooter, true);
    }

    private String getOptionalParam(String commandLineKey, String fallbackProperty) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        String propertyValue = props.getProperty(fallbackProperty);
        return StringUtils.isNotEmpty(commandLineValue) ? commandLineValue : propertyValue;
    }

    private static String getRequiredParam(
            CommandLine cmdLine, String cmdLineOptionName, @Nullable String fallbackProperty) 
            throws CLIParsingException {
        String result = cmdLine.getOptionValue(cmdLineOptionName);
        if (Strings.isNullOrEmpty(result) && fallbackProperty != null) {
            result = props.getProperty(fallbackProperty);
        }

        if (Strings.isNullOrEmpty(result)) {
            String message;
            if (fallbackProperty != null) {
                message = String.format("%s command line option or %s configuration setting must be specified.",
                        cmdLineOptionName, fallbackProperty);
            }
            else {
                message = String.format("%s command line option must be specified.", cmdLineOptionName);
            }
            throw new CLIParsingException(message);
        }
        return result;
    }

    private static String normalizeUrl(String rawValue) {
        return rawValue.startsWith("http") ? rawValue : "http://" + rawValue;
    }
}
