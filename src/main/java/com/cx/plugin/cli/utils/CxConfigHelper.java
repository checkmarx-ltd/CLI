package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.exceptions.BadOptionCombinationException;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.ast.dto.sca.AstScaConfig;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.SourceLocationType;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Created by idanA on 11/5/2018.
 */
public final class CxConfigHelper {

    private static Logger log = LoggerFactory.getLogger(CxConfigHelper.class);

    private static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static PropertiesManager props;

    private Command command;
    private CommandLine commandLine;

    public CxConfigHelper(String configFilePath) {
        props = PropertiesManager.getProps(configFilePath);
    }

    /**
     * Resolves configuration from the config file and the console parameters
     *
     * @param command the first command line argument mapped to an enum.
     * @param cmd     the parameters passed by the user mapped by key/value
     * @return CxScanConfig an object containing all the relevant data for the scan
     */
    public CxScanConfig resolveConfiguration(Command command, CommandLine cmd) throws CLIParsingException {
        this.command = command;
        this.commandLine = cmd;

        checkForAmbiguousArgs();

        CxScanConfig scanConfig = new CxScanConfig();

        if (testConnection(scanConfig)) {
            return scanConfig;
        }

        scanConfig.setProxyConfig(genProxyConfig());
        scanConfig.setNTLM(cmd.hasOption(NTLM));

        if (command.equals(Command.SCAN) || command.equals(Command.ASYNC_SCAN)) {
            scanConfig.setSastEnabled(true);
            scanConfig.addScannerType(ScannerType.SAST);

        }

        scanConfig.setSynchronous(command.equals(Command.SCAN) ||
                command.equals(Command.OSA_SCAN) ||
                command.equals(Command.SCA_SCAN));


        ScannerType scannerType = getDependencyScannerType();

        if (scannerType != null)
            scanConfig.addScannerType(scannerType);


        scanConfig.setCxOrigin(CX_ORIGIN);

        if (scanConfig.isSastOrOSAEnabled() || command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            scanConfig.setUrl(getSastOrOsaServerUrl());
            setSastOrOsaCredentials(scanConfig);
        }

        if (command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            return scanConfig;
        }

        scanConfig.setEngineConfigurationName(cmd.getOptionValue(CONFIGURATION));

        scanConfig.setUseSSOLogin(cmd.hasOption(IS_SSO));
        scanConfig.setDisableCertificateValidation(cmd.hasOption(TRUSTED_CERTIFICATES));

        scanConfig.setPublic(!cmd.hasOption(IS_PRIVATE));
        scanConfig.setEnablePolicyViolations(cmd.hasOption(IS_CHECKED_POLICY));
        if ((command.equals(Command.SCA_SCAN)) || (command.equals(Command.ASYNC_SCA_SCAN))) {
            scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH), true));
        } else {
            scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH), false));
            scanConfig.setTeamPath(extractTeamPath(cmd.getOptionValue(FULL_PROJECT_PATH)));
        }
        scanConfig.setPresetName(cmd.getOptionValue(PRESET) == null ? DEFAULT_PRESET_NAME : cmd.getOptionValue(PRESET));

        scanConfig.setSastFolderExclusions(getParamWithDefault(LOCATION_PATH_EXCLUDE, KEY_EXCLUDED_FOLDERS));
        scanConfig.setSastFilterPattern(getParamWithDefault(LOCATION_FILES_EXCLUDE, KEY_EXCLUDED_FILES));
        scanConfig.setScanComment(cmd.getOptionValue(SCAN_COMMENT));
        setScanReports(scanConfig);
        scanConfig.setIncremental(cmd.hasOption(IS_INCREMENTAL));
        scanConfig.setForceScan(cmd.hasOption(IS_FORCE_SCAN));
        setSASTThresholds(scanConfig);

        String dsLocationPath = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        if (scanConfig.isSastEnabled() || dsLocationPath == null) {
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
        if (!scanConfig.isAstScaEnabled() && !scanConfig.isOsaEnabled()) {
            return;
        }

        if (scanConfig.isOsaEnabled()) {
            setOsaSpecificConfig(scanConfig);
        } else if (scanConfig.isAstScaEnabled()) {
            setScaSpecificConfig(scanConfig);
        }

        setSharedDependencyScanConfig(scanConfig);
    }

    private void setOsaSpecificConfig(CxScanConfig scanConfig) {
        scanConfig.setOsaRunInstall(commandLine.hasOption(INSTALL_PACKAGE_MANAGER));

        String reportDir = commandLine.getOptionValue(OSA_JSON_REPORT);
        scanConfig.setReportsDir(reportDir != null ? new File(reportDir) : null);
        scanConfig.setOsaGenerateJsonReport(reportDir != null);

        String archiveIncludePatterns = getParamWithDefault(OSA_ARCHIVE_TO_EXTRACT, KEY_OSA_EXTRACTABLE_INCLUDE_FILES);
        scanConfig.setOsaArchiveIncludePatterns(archiveIncludePatterns);

        String osaScanDepth = getOptionalParam(OSA_SCAN_DEPTH, KEY_OSA_SCAN_DEPTH);
        scanConfig.setOsaScanDepth(osaScanDepth);
    }


    private void setScaSpecificConfig(CxScanConfig scanConfig) throws CLIParsingException {
        AstScaConfig sca = new AstScaConfig();

        String apiUrl = normalizeUrl(getRequiredParam(commandLine, SCA_API_URL, KEY_SCA_API_URL));
        sca.setApiUrl(apiUrl);

        String acUrl = normalizeUrl(getRequiredParam(commandLine, SCA_ACCESS_CONTROL_URL, KEY_SCA_ACCESS_CONTROL_URL));
        sca.setAccessControlUrl(acUrl);

        String webAppUrl = normalizeUrl(getRequiredParam(commandLine, SCA_WEB_APP_URL, KEY_SCA_WEB_APP_URL));
        sca.setWebAppUrl(webAppUrl);

        sca.setUsername(getRequiredParam(commandLine, SCA_USERNAME, null));
        sca.setPassword(getRequiredParam(commandLine, SCA_PASSWORD, null));
        sca.setTenant(getRequiredParam(commandLine, SCA_ACCOUNT, null));

        sca.setRemoteRepositoryInfo(null);
        sca.setSourceLocationType(SourceLocationType.LOCAL_DIRECTORY);

        String reportDir = commandLine.getOptionValue(SCA_JSON_REPORT);
        File reportFolder = new File(reportDir);
        Path reportPath = Paths.get(reportDir);
        if(reportFolder.isDirectory() && reportFolder.canWrite()) {
            if(Files.isWritable(reportPath)){
            scanConfig.setReportsDir(reportDir != null ? reportFolder : null);
            //use setOsaGenerateJsonReport instead of creating one for sca, because there is no case of using osa and sca simultaneously.
            scanConfig.setOsaGenerateJsonReport(reportDir != null);
            scanConfig.setScaJsonReport(reportDir);
            }else{
                throw new CLIParsingException("There is no write access for: " + reportDir);
            }
        }else{
            throw new CLIParsingException(reportDir + " directory doesn't exist.");
        }

        scanConfig.setAstScaConfig(sca);
    }

    private void setSharedDependencyScanConfig(CxScanConfig scanConfig) {
        setDependencyScanThresholds(scanConfig);

        String location = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        scanConfig.setOsaLocationPath(location);

        String folderExclusions = getSharedDependencyScanOption(scanConfig, OSA_FOLDER_EXCLUDE, SCA_FOLDER_EXCLUDE);
        scanConfig.setOsaFolderExclusions(folderExclusions);

        String key = scanConfig.isOsaEnabled() ?
                KEY_OSA_PROGRESS_INTERVAL :
                KEY_SCA_PROGRESS_INTERVAL;
        scanConfig.setOsaProgressInterval(props.getIntProperty(key));

        setDependencyScanFilterPattern(scanConfig);
    }

    private void setDependencyScanFilterPattern(CxScanConfig scanConfig) {
        String includedFiles, excludedFiles;
        if (scanConfig.isOsaEnabled()) {
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
        String password = validatePassword(commandLine.getOptionValue(USER_PASSWORD));
        if (isNotEmpty(token)) {
            scanConfig.setRefreshToken(token);
        } else if ((Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) && !isSSO) {
            throw new CLIParsingException("[CxConsole] User name and password are mandatory unless SSO or token is used");
        } else {
            scanConfig.setUsername(username);
            scanConfig.setPassword(password);
        }
    }

    private String validatePassword(String password) {
        return (StringUtils.isNotEmpty(password) && password.startsWith("''")) ? password.substring(2) : password;
    }

    private String getSastOrOsaServerUrl() throws CLIParsingException {
        String rawUrl = getRequiredParam(commandLine, SERVER_URL, null);
        return normalizeUrl(rawUrl);
    }

    private void checkForAmbiguousArgs() throws CLIParsingException {
        String message = null;
        if (commandLine.hasOption(OSA_ENABLED) && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s and %s arguments cannot be specified together.", OSA_ENABLED, SCA_ENABLED);
        } else if (command == Command.SCA_SCAN && commandLine.hasOption(OSA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", OSA_ENABLED, command);
        } else if (command == Command.OSA_SCAN && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", SCA_ENABLED, command);
        }

        if (message != null) {
            throw new BadOptionCombinationException(message);
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
        if (scanConfig.isOsaEnabled()) {
            result = commandLine.getOptionValue(osaOption);
        } else if (scanConfig.isAstScaEnabled()) {
            result = commandLine.getOptionValue(scaOption);
        }
        return result;
    }

    private ScannerType getDependencyScannerType() {
        ScannerType result;
        if (command == Command.OSA_SCAN || command == Command.ASYNC_OSA_SCAN || commandLine.hasOption(OSA_ENABLED)) {
            result = ScannerType.OSA;
        } else if (command == Command.SCA_SCAN || command == Command.ASYNC_SCA_SCAN || commandLine.hasOption(SCA_ENABLED)) {
            result = ScannerType.AST_SCA;
        } else {
            result = null;
        }
        return result;
    }

    private static int getLastIndexOfTeam(String fullPath, boolean isScaScan) throws CLIParsingException {
        int lastIdxOfBackSlash = fullPath.lastIndexOf("\\");
        int lastIdxOfSlash = fullPath.lastIndexOf("/");
        if (lastIdxOfBackSlash == -1 && lastIdxOfSlash == -1) {
            if (isScaScan) {
                return -1; // so it takes the name from index 0, which is the start if the project name
            }
            throw new CLIParsingException(String.format("[CxConsole]  Invalid project path specified: %s", fullPath));
        }
        return lastIdxOfBackSlash != -1 ? lastIdxOfBackSlash : lastIdxOfSlash;
    }

    private static String extractProjectName(String fullPath, boolean isScaScan) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath, isScaScan);
        return fullPath.substring(lastIdx + 1);
    }

    private static String extractTeamPath(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath, false);
        return fullPath.substring(0, lastIdx);
    }

    public static void printConfig(CommandLine commandLine) {
        log.info("-----------------------------------------------------------------------------------------");
        log.info("CxConsole Configuration: ");
        log.info("--------------------");
        for (Option param : commandLine.getOptions()) {
            String name = param.getLongOpt() != null ? param.getLongOpt() : param.getOpt();
            String value;
            if (param.getOpt().equalsIgnoreCase(Parameters.USER_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(SCA_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(LOCATION_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(TOKEN)) {
                value = "********";
            } else if (param.getOpt().equalsIgnoreCase(USER_NAME) ||
                    param.getOpt().equalsIgnoreCase(SCA_USERNAME) ||
                    param.getOpt().equalsIgnoreCase(LOCATION_USER)) {
                value = param.getValue();
                log.debug("{}: {}", name, value);
                value = DigestUtils.sha256Hex(param.getValue());
            } else if (param.hasArg()) {
                value = param.getValue();
            } else {
                value = "true";
            }
            log.info("{}: {}", name, value);
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
        helpFormatter.printHelp(120, mode.value(), helpHeader, Command.getOptions(), helpFooter, true);
    }

    private String getOptionalParam(String commandLineKey, String fallbackProperty) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        String propertyValue = props.getProperty(fallbackProperty);
        return isNotEmpty(commandLineValue) ? commandLineValue : propertyValue;
    }

    private String getParamWithDefault(String commandLineKey, String fallbackProperty) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        String propertyValue = props.getProperty(fallbackProperty);
        return isNotEmpty(commandLineValue) ? propertyValue + ", " + commandLineValue : propertyValue;
    }

    private static String getRequiredParam(CommandLine cmdLine, String cmdLineOptionName, @Nullable String fallbackProperty)
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
            } else {
                message = String.format("%s command line option must be specified.", cmdLineOptionName);
            }
            throw new CLIParsingException(message);
        }
        return result;
    }

    private static String normalizeUrl(String rawValue) {
        return rawValue.startsWith("http") ? rawValue : "http://" + rawValue;
    }

    private ProxyConfig genProxyConfig() {
        final String HTTP_HOST = System.getProperty("http.proxyHost");
        final String HTTP_PORT = System.getProperty("http.proxyPort");
        final String HTTP_USERNAME = System.getProperty("http.proxyUser");
        final String HTTP_PASSWORD = System.getProperty("http.proxyPassword");

        final String HTTPS_HOST = System.getProperty("https.proxyHost");
        final String HTTPS_PORT = System.getProperty("https.proxyPort");
        final String HTTPS_USERNAME = System.getProperty("https.proxyUser");
        final String HTTPS_PASSWORD = System.getProperty("https.proxyPassword");

        ProxyConfig proxyConfig = null;
        try {
            if (isNotEmpty(HTTP_HOST) && isNotEmpty(HTTP_PORT)) {
                proxyConfig = new ProxyConfig();
                proxyConfig.setUseHttps(false);
                proxyConfig.setHost(HTTP_HOST);
                proxyConfig.setPort(Integer.parseInt(HTTP_PORT));
                if (isNotEmpty(HTTP_USERNAME) && isNotEmpty(HTTP_PASSWORD)) {
                    proxyConfig.setUsername(HTTP_USERNAME);
                    proxyConfig.setPassword(HTTP_PASSWORD);
                }
            } else if (isNotEmpty(HTTPS_HOST) && isNotEmpty(HTTPS_PORT)) {
                proxyConfig = new ProxyConfig();
                proxyConfig.setUseHttps(true);
                proxyConfig.setHost(HTTPS_HOST);
                proxyConfig.setPort(Integer.parseInt(HTTPS_PORT));
                if (isNotEmpty(HTTPS_USERNAME) && isNotEmpty(HTTPS_PASSWORD)) {
                    proxyConfig.setUsername(HTTPS_USERNAME);
                    proxyConfig.setPassword(HTTPS_PASSWORD);
                }
            }
        } catch (Exception e) {
            log.error("Fail to set custom proxy", e);
        }

        return proxyConfig;
    }

    private boolean testConnection(CxScanConfig scanConfig) throws CLIParsingException {
        if (command.equals(Command.TEST_CONNECTION)) {
            if (commandLine.getOptionValue(SERVER_URL) != null) {
                scanConfig.setUrl(getSastOrOsaServerUrl());
                setSastOrOsaCredentials(scanConfig);
                scanConfig.setUseSSOLogin(commandLine.hasOption(IS_SSO));
            } else {
                setScaSpecificConfig(scanConfig);
            }
            return true;
        }
        return false;
    }
}
