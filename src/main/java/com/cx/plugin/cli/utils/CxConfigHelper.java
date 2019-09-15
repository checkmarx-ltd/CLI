package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.RemoteSourceTypes;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.KEY_OSA_SCAN_DEPTH;

/**
 * Created by idanA on 11/5/2018.
 */
public final class CxConfigHelper {

    private static final String SVN_DEFAULT_PORT = "80";
    private static final String PERFORCE_DEFAULT_PORT = "1666";
    private static final String TFS_DEFAULT_PORT = "8080";

    private static Logger log = LoggerFactory.getLogger(CxConfigHelper.class);

    private static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static PropertiesManager props;

    private CxConfigHelper() {
        props = PropertiesManager.getProps();
    }

    /**
     * Resolves configuration from the config file and the console parameters
     *
     * @param command
     * @param cmd     - The parameters passed by the user mapped by key/value
     * @return CxScanConfig an object containing all the relevant data for the scan
     */
    //TODO: use props where user param missing
    public static CxScanConfig resolveConfigurations(Command command, CommandLine cmd) throws CLIParsingException, URISyntaxException {
        CxScanConfig scanConfig = new CxScanConfig();
        setScanMode(command, scanConfig);
        scanConfig.setCxOrigin(CX_ORIGIN);

        if (Strings.isNullOrEmpty(cmd.getOptionValue(SERVER_URL))) {
            throw new CLIParsingException(String.format("[CxConsole] %s parameter must be specified", SERVER_URL));
        }
        String serverURL = cmd.getOptionValue(SERVER_URL);
        if (!serverURL.contains("http")) {
            cmd.getOptionValue(SERVER_URL, "http://" + serverURL);
        }

        scanConfig.setUrl(cmd.getOptionValue(SERVER_URL));
        boolean isSSO = cmd.getOptionValue(IS_SSO) != null;
        String token = cmd.getOptionValue(TOKEN);
        if (!Strings.isNullOrEmpty(token)) {
            scanConfig.setRefreshToken(token);
        } else if ((Strings.isNullOrEmpty(cmd.getOptionValue(USER_NAME)) || Strings.isNullOrEmpty(cmd.getOptionValue(USER_PASSWORD))) && !isSSO) {
            throw new CLIParsingException("[CxConsole] User name and password are mandatory unless SSO or token is used");
        } else {
            scanConfig.setUsername(cmd.getOptionValue(USER_NAME));
            scanConfig.setPassword(cmd.getOptionValue(USER_PASSWORD));
        }

        if (command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            return scanConfig;
        }

        scanConfig.setUseSSOLogin(isSSO);

        scanConfig.setOsaEnabled(cmd.hasOption(OSA_ENABLED) || cmd.getOptionValue(OSA_LOCATION_PATH) != null);
        scanConfig.setOsaRunInstall(cmd.hasOption(INSTALL_PACKAGE_MANAGER));
        scanConfig.setPublic(cmd.hasOption(IS_PRIVATE));
        scanConfig.setEnablePolicyViolations(cmd.hasOption(IS_CHECKED_POLICY));
        scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setTeamPath(extractTeamPath(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setPresetName(cmd.getOptionValue(PRESET) == null ? DEFAULT_PRESET_NAME : cmd.getOptionValue(PRESET));

//        scanConfig.setSastFolderExclusions(params.get(LOCATION_PATH_EXCLUDE) == null ? props.getProperty(LOCATION_PATH_EXCLUDE));
        scanConfig.setSastFilterPattern(cmd.getOptionValue(LOCATION_FILES_EXCLUDE));
        scanConfig.setScanComment(cmd.getOptionValue(SCAN_COMMENT));
        setScanReports(cmd, scanConfig);
        scanConfig.setIncremental(cmd.hasOption(IS_INCREMENTAL));
        scanConfig.setForceScan(cmd.hasOption(IS_FORCE_SCAN));
        setSASTThreshold(cmd, scanConfig);

        setSourceLocation(cmd, scanConfig);
        if (scanConfig.getOsaEnabled()) {
            setOSAThreshold(cmd, scanConfig);
            //TODO: should be able to choose the location of the report
            scanConfig.setOsaGenerateJsonReport(cmd.getOptionValue(OSA_JSON_REPORT) != null);
        }
        return scanConfig;
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

    private static CxScanConfig setSourceLocation(CommandLine cmd, CxScanConfig scanConfig) throws CLIParsingException {
        if (cmd.getOptionValue(OSA_LOCATION_PATH) != null) {
            return scanConfig;
        }

        String locationType = cmd.getOptionValue(LOCATION_TYPE);
        if (locationType == null) {
            throw new CLIParsingException("[CxConsole] No location type parameter was specified, needs to be specified using -LocationType <folder/shared/tfs/svn/perforce/git>");
        }

        final String locationPath = cmd.getOptionValue(LOCATION_PATH);
        final String locationURL = cmd.getOptionValue(LOCATION_URL);
        final String locationBranch = cmd.getOptionValue(LOCATION_BRANCH);
        final String locationUser = cmd.getOptionValue(LOCATION_USER);
        final String locationPass = cmd.getOptionValue(LOCATION_PASSWORD);

        String locationPort = cmd.getOptionValue(LOCATION_PORT);

        switch (locationType.toLowerCase()) {
            case "folder": {
                setLocalSourceLocation(scanConfig, locationPath);
                break;
            }
            case "shared": {
                setSharedSourceLocation(scanConfig, locationPath, locationUser, locationPass);
                break;
            }
            case "svn": {
                if (cmd.getOptionValue(PRIVATE_KEY) != null) {
                    setPrivateKey(scanConfig, cmd.getOptionValue(PRIVATE_KEY));
                }
                setSVNSourceLocation(scanConfig, locationURL, locationPath, locationUser, locationPass, locationPort);
                break;
            }
            case "tfs": {
                setTFSSourceLocation(scanConfig, locationURL, locationPath, locationUser, locationPass, locationPort);
                break;
            }
            case "perforce": {
                setPerforceSourceLocation(scanConfig, locationURL, locationPath, locationUser, locationPass, locationPort);
                break;
            }
            case "git": {
                if (cmd.getOptionValue(PRIVATE_KEY) != null) {
                    setPrivateKey(scanConfig, cmd.getOptionValue(PRIVATE_KEY));
                }
                setGITSourceLocation(scanConfig, locationURL, locationBranch, locationUser, locationPass, locationPort);
                break;
            }
            default: {
                throw new CLIParsingException(String.format("[CxConsole] location type must be one of " +
                        "[folder, shared, (network location), SVN, TFS, Perfore, GIT] but was %s", locationType));
            }
        }

        return scanConfig;

    }

    private static CxScanConfig setPrivateKey(CxScanConfig scanConfig, String pathToKey) throws CLIParsingException {
        File resultFile = new File(pathToKey);
        if (!resultFile.isAbsolute()) {
            String path = System.getProperty("user.dir");
            pathToKey = path + File.separator + pathToKey;
        }

        byte[] keyFile;
        try {
            FileInputStream inputStream = new FileInputStream(new File(pathToKey));
            keyFile = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new CLIParsingException("[CxConsole] Failed retrieving key from specified file:\n " + e.getMessage(), e);
        }

        scanConfig.setRemoteSrcKeyFile(keyFile);
        return scanConfig;
    }

    private static CxScanConfig setLocalSourceLocation(CxScanConfig scanConfig, String locationPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format("[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType", "folder"));
        }
        scanConfig.setSourceDir(locationPath);
        //scanConfig.setZipMaxFile(maxSize);
        return scanConfig;
    }

    private static CxScanConfig setSharedSourceLocation(CxScanConfig scanConfig, String locationPath, String locationUser, String locationPass) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format("[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.SHARED.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format("[CxConsole] locationUser parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.SHARED.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format("[CxConsole] locationPassword parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.SHARED.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(locationPass);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setRemoteType(RemoteSourceTypes.SHARED);

        return scanConfig;
    }

    private static CxScanConfig setSVNSourceLocation(CxScanConfig scanConfig, String locationURL, String locationPath, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format("[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.SVN.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format("[CxConsole] locationURL parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.SVN.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(String.valueOf(locationPass));
        scanConfig.setRemoteType(RemoteSourceTypes.SVN);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setRemoteSrcUrl(locationURL);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info(String.format("[CxConsole] No port was specified for SVN, using port %s as default", SVN_DEFAULT_PORT));
            locationPort = SVN_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format("[CxConsole] Invalid port specified: [%s]", locationPort), e);
        }

        return scanConfig;
    }

    private static CxScanConfig setTFSSourceLocation(CxScanConfig scanConfig, String locationURL, String locationPath, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format("[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.TFS.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format("[CxConsole] locationURL parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.TFS.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format("[CxConsole] locationUser parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.TFS.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format("[CxConsole] locationPassword parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.TFS.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(String.valueOf(locationPass));
        scanConfig.setRemoteType(RemoteSourceTypes.TFS);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setRemoteSrcUrl(locationURL);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info(String.format("[CxConsole] No port was specified for TFS, using port %s as default", TFS_DEFAULT_PORT));
            locationPort = TFS_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format("[CxConsole] Invalid port specified: [%s]", locationPort), e);
        }

        return scanConfig;
    }

    //TODO: perforce workspaceMode should it be in the common or CLI?
    private static CxScanConfig setPerforceSourceLocation(CxScanConfig scanConfig, String locationURL, String locationPath, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format("[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.PERFORCE.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format("[CxConsole] locationURL parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.PERFORCE.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format("[CxConsole] locationUser parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.PERFORCE.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format("[CxConsole] locationPassword parameter needs to be specified when using [%s] as locationType", RemoteSourceTypes.PERFORCE.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(locationPass);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setRemoteSrcUrl(locationURL);
        scanConfig.setRemoteType(RemoteSourceTypes.PERFORCE);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info(String.format("[CxConsole] No port was specified for perforce, using port %s as default", PERFORCE_DEFAULT_PORT));
            locationPort = PERFORCE_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format("[CxConsole] Invalid port specified: [%s]", locationPort), e);
        }

        return scanConfig;
    }

    private static CxScanConfig setGITSourceLocation(CxScanConfig scanConfig, String locationURL, String locationBranch, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format("[CxConsole] locationURL parameter needs to be specified when using [%s] as a locationType", RemoteSourceTypes.GIT.value()));
        }
        if (Strings.isNullOrEmpty(locationBranch)) {
            throw new CLIParsingException(String.format("[CxConsole] locationBranch parameter needs to be specified when using [%s] as a locationType", RemoteSourceTypes.GIT.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(String.valueOf(locationPass));
        scanConfig.setRemoteType(RemoteSourceTypes.GIT);
        scanConfig.setRemoteSrcUrl(locationURL);
        scanConfig.setRemoteSrcBranch(locationBranch);
        try {
            if (!Strings.isNullOrEmpty(locationPort)) {
                scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
            }
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format("[CxConsole] Invalid port specified: [%s]", locationPort), e);
        }

        return scanConfig;
    }

    //TODO: generating multiple reports for multiple types?
    private static CxScanConfig setScanReports(CommandLine consoleParams, CxScanConfig scanConfig) throws URISyntaxException {
        if (consoleParams.getOptionValue(PDF_REPORT) != null) {
            String reportPath = consoleParams.getOptionValue(PDF_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.setReportsDir(new File(reportPath));
            scanConfig.setGeneratePDFReport(true);
        }
        if (consoleParams.getOptionValue(XML_REPORT) != null) {
            String reportPath = consoleParams.getOptionValue(XML_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.setReportsDir(new File(reportPath));
            scanConfig.setGeneratePDFReport(true);
        }
        if (consoleParams.getOptionValue(CSV_REPORT) != null) {
            String reportPath = consoleParams.getOptionValue(CSV_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.setReportsDir(new File(reportPath));
            scanConfig.setGeneratePDFReport(true);
        }
        if (consoleParams.getOptionValue(RTF_REPORT) != null) {
            String reportPath = consoleParams.getOptionValue(RTF_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.setReportsDir(new File(reportPath));
            scanConfig.setGeneratePDFReport(true);
        }

        return scanConfig;
    }

    private static CxScanConfig setSASTThreshold(CommandLine cmd, CxScanConfig scanConfig) {
        String sastHigh = cmd.getOptionValue(SAST_HIGH);
        String sastMedium = cmd.getOptionValue(SAST_MEDIUM);
        String sastLow = cmd.getOptionValue(SAST_LOW);

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

    private static CxScanConfig setOSAThreshold(CommandLine cmd, CxScanConfig scanConfig) {
        String osaHigh = cmd.getOptionValue(OSA_HIGH);
        String osaMedium = cmd.getOptionValue(OSA_MEDIUM);
        String osaLow = cmd.getOptionValue(OSA_LOW);

        scanConfig.setOsaThresholdsEnabled(false);
        if (!Strings.isNullOrEmpty(osaHigh)) {
            scanConfig.setOsaHighThreshold(Integer.valueOf(osaHigh));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(osaMedium)) {
            scanConfig.setOsaMediumThreshold(Integer.valueOf(osaMedium));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(osaLow)) {
            scanConfig.setOsaLowThreshold(Integer.valueOf(osaLow));
            scanConfig.setOsaThresholdsEnabled(true);
        }

        return scanConfig;
    }

    private static CxScanConfig setScanMode(Command mode, CxScanConfig scanConfig) {
        switch (mode) {
            case SCAN:
                scanConfig.setSastEnabled(true);
                scanConfig.setSynchronous(true);
                break;
            case ASYNC_SCAN:
                scanConfig.setSastEnabled(true);
                scanConfig.setSynchronous(false);
                break;
            case OSA_SCAN:
                scanConfig.setOsaEnabled(true);
                scanConfig.setSynchronous(true);
                break;
            case ASYNC_OSA_SCAN:
                scanConfig.setOsaEnabled(true);
                scanConfig.setSynchronous(false);
                break;
//            TODO: verify flow exists in common
            case GENERATE_TOKEN:
                break;
            case REVOKE_TOKEN:
                break;
        }

        return scanConfig;
    }

    private static String extractProjectName(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        String[] split = fullPath.split("\\\\");
        return split[split.length - 1];
    }

    //TODO: make sure that it works with the reverse slashes
    private static String extractTeamPath(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = fullPath.lastIndexOf("\\");
        return fullPath.substring(0, lastIdx);
    }

    //TODO: can remove this
    private static Properties generateFSAConfig(Map<String, String> consoleParams, PropertiesManager propertiesManager) {
        Properties fsaConfig = new Properties();

        String osaDirectoriesToAnalyze = consoleParams.get(OSA_LOCATION_PATH) != null ? consoleParams.get(OSA_LOCATION_PATH) : consoleParams.get(LOCATION_PATH);
        String osaFolderExcludeString = consoleParams.get(OSA_FOLDER_EXCLUDE) != null ? consoleParams.get(OSA_FOLDER_EXCLUDE) : "";
        String osaFilesExcludesString = consoleParams.get(OSA_FILES_EXCLUDE) != null ? consoleParams.get(OSA_FILES_EXCLUDE) : propertiesManager.getProperty("scan.osa.exclude.files");
        String osaFilesIncludesString = consoleParams.get(OSA_FILES_INCLUDE) != null ? consoleParams.get(OSA_FILES_INCLUDE) : propertiesManager.getProperty("scan.osa.include.files");
        String osaExtractableIncludesString = consoleParams.get(OSA_FOLDER_EXCLUDE) != null ? consoleParams.get(OSA_ARCHIVE_TO_EXTRACT) : "";
        String archiveExtractDepth = consoleParams.get(OSA_SCAN_DEPTH) != null ? consoleParams.get(OSA_SCAN_DEPTH) : propertiesManager.getProperty(KEY_OSA_SCAN_DEPTH);

        StringBuilder osaExcludes = new StringBuilder();
        if (!osaFolderExcludeString.isEmpty()) {
            osaExcludes.append(osaFolderExcludeString).append(" ");
        }
        if (!Strings.isNullOrEmpty(osaFilesExcludesString)) {
            osaExcludes.append(osaFilesExcludesString);
        }

        if (!osaExcludes.toString().isEmpty()) {
            fsaConfig.put("excludes", osaExcludes.toString().trim());
        }

        fsaConfig.put("includes", osaFilesIncludesString);
        fsaConfig.put("archiveIncludes", osaExtractableIncludesString);
        fsaConfig.put("archiveExtractionDepth", archiveExtractDepth);

        String shouldPackageManagerInstall = "false";
        if (consoleParams.get(INSTALL_PACKAGE_MANAGER) != null) {
            fsaConfig.put("npm.runPreStep", "true");
            fsaConfig.put("npm.ignoreScripts", "true");
            fsaConfig.put("bower.runPreStep", "false");
            shouldPackageManagerInstall = "true";
        }

        fsaConfig.put("nuget.resolveDependencies", shouldPackageManagerInstall);
        fsaConfig.put("nuget.restoreDependencies", shouldPackageManagerInstall);
        fsaConfig.put("python.resolveDependencies", shouldPackageManagerInstall);
        fsaConfig.put("python.ignorePipInstallErrors", shouldPackageManagerInstall);

//        fsaConfig.put("acceptExtensionsList", ACCEPT_FSA_EXTENSIONS_LISTS);
        fsaConfig.put("d", osaDirectoriesToAnalyze);
//        if (consoleParams.get(DOCKER_IMAGE_PATTERN) != null) {
//            fsaConfig.put("followSymbolicLinks", "false");
//            fsaConfig.put("docker.scanImages", "true");
//            fsaConfig.put("docker.includes", consoleParams.get(DOCKER_IMAGE_PATTERN));
//            fsaConfig.put("docker.excludes", consoleParams.get(DOCKER_EXCLUDE));
//        }

        return fsaConfig;
    }

}
