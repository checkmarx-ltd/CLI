package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.DependencyScannerType;
import com.cx.restclient.dto.RemoteSourceTypes;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.*;

/**
 * Created by idanA on 11/5/2018.
 */
public final class CxConfigHelper {

    private static final String SVN_DEFAULT_PORT = "80";
    private static final String PERFORCE_DEFAULT_PORT = "1666";
    private static final String TFS_DEFAULT_PORT = "8080";

    private static Logger log = LoggerFactory.getLogger(CxConfigHelper.class);

    private static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static PropertiesManager props = PropertiesManager.getProps();

    private CxConfigHelper() {
    }

    /**
     * Resolves configuration from the config file and the console parameters
     *
     * @param command
     * @param cmd     - The parameters passed by the user mapped by key/value
     * @return CxScanConfig an object containing all the relevant data for the scan
     */
    public static CxScanConfig resolveConfigurations(Command command, CommandLine cmd) throws CLIParsingException, URISyntaxException {
        CxScanConfig scanConfig = new CxScanConfig();
        setScanMode(command, cmd, scanConfig);
        scanConfig.setCxOrigin(CX_ORIGIN);

        if (Strings.isNullOrEmpty(cmd.getOptionValue(SERVER_URL))) {
            throw new CLIParsingException(String.format("[CxConsole] %s parameter must be specified", SERVER_URL));
        }
        String serverURL = cmd.getOptionValue(SERVER_URL);
        scanConfig.setUrl(!serverURL.substring(0, 4).contains("http") ? "http://" + serverURL : cmd.getOptionValue(SERVER_URL));

        boolean isSSO = cmd.hasOption(IS_SSO);
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

        scanConfig.setUseSSOLogin(cmd.hasOption(IS_SSO));

        scanConfig.setOsaRunInstall(cmd.hasOption(INSTALL_PACKAGE_MANAGER));
        scanConfig.setPublic(cmd.hasOption(IS_PRIVATE));
        scanConfig.setEnablePolicyViolations(cmd.hasOption(IS_CHECKED_POLICY));
        scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setTeamPath(extractTeamPath(cmd.getOptionValue(FULL_PROJECT_PATH)));
        scanConfig.setPresetName(cmd.getOptionValue(PRESET) == null ? DEFAULT_PRESET_NAME : cmd.getOptionValue(PRESET));

        scanConfig.setSastFolderExclusions(cmd.getOptionValue(LOCATION_PATH_EXCLUDE) == null ? props.getProperty(KEY_EXCLUDED_FOLDERS) : cmd.getOptionValue(LOCATION_PATH_EXCLUDE));
        scanConfig.setSastFilterPattern(cmd.getOptionValue(LOCATION_FILES_EXCLUDE) == null ? props.getProperty(KEY_EXCLUDED_FILES) : cmd.getOptionValue(LOCATION_FILES_EXCLUDE));
        scanConfig.setScanComment(cmd.getOptionValue(SCAN_COMMENT));
        setScanReports(cmd, scanConfig);
        scanConfig.setIncremental(cmd.hasOption(IS_INCREMENTAL));
        scanConfig.setForceScan(cmd.hasOption(IS_FORCE_SCAN));
        setSASTThreshold(cmd, scanConfig);

        setSourceLocation(cmd, scanConfig);
        if (scanConfig.getDependencyScannerType() != DependencyScannerType.NONE) {
            setOSAThreshold(cmd, scanConfig);
            scanConfig.setReportsDir(cmd.getOptionValue(OSA_JSON_REPORT) != null ? new File(cmd.getOptionValue(OSA_JSON_REPORT)) : null);
            scanConfig.setOsaLocationPath(cmd.getOptionValue(OSA_LOCATION_PATH));
            scanConfig.setOsaGenerateJsonReport(cmd.getOptionValue(OSA_JSON_REPORT) != null);
            scanConfig.setOsaProgressInterval(props.getIntProperty(KEY_OSA_PROGRESS_INTERVAL));

            scanConfig.setOsaFolderExclusions(cmd.getOptionValue(OSA_FOLDER_EXCLUDE));

            String osaIncludedFiles = Strings.isNullOrEmpty(cmd.getOptionValue(OSA_FILES_INCLUDE)) ? props.getProperty(KEY_OSA_INCLUDED_FILES) : cmd.getOptionValue(OSA_FILES_INCLUDE);
            String osaExcludedFiles = Strings.isNullOrEmpty(cmd.getOptionValue(OSA_FILES_EXCLUDE)) ? props.getProperty(KEY_OSA_EXCLUDED_FILES) : cmd.getOptionValue(OSA_FILES_EXCLUDE);

            String osaFilterPattern = null;
            if (osaIncludedFiles != null) {
                if (osaExcludedFiles != null) {
                    osaFilterPattern = osaIncludedFiles + ", " + osaExcludedFiles;
                }
                else
                    osaFilterPattern = osaIncludedFiles;
            }
            else if (osaExcludedFiles != null) {
                osaFilterPattern = osaExcludedFiles;
            }

            scanConfig.setOsaFilterPattern(osaFilterPattern);

            String osaExtractableIncludeFiles = Strings.isNullOrEmpty(cmd.getOptionValue(OSA_ARCHIVE_TO_EXTRACT)) ? props.getProperty(KEY_OSA_EXTRACTABLE_INCLUDE_FILES) : cmd.getOptionValue(OSA_ARCHIVE_TO_EXTRACT);
            scanConfig.setOsaArchiveIncludePatterns(osaExtractableIncludeFiles);

            String osaScanDepth = Strings.isNullOrEmpty(cmd.getOptionValue(OSA_SCAN_DEPTH)) ? props.getProperty(KEY_OSA_SCAN_DEPTH) : cmd.getOptionValue(OSA_SCAN_DEPTH);
            scanConfig.setOsaScanDepth(osaScanDepth);
        }

        scanConfig.setProgressInterval(props.getIntProperty(KEY_PROGRESS_INTERVAL));
        scanConfig.setConnectionRetries(props.getIntProperty(KEY_RETIRES));
        scanConfig.setDefaultProjectName(props.getProperty(KEY_DEF_PROJECT_NAME));

        return scanConfig;
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
        final String workspaceMode = cmd.getOptionValue(WORKSPACE_MODE);

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
                setPerforceSourceLocation(scanConfig, locationURL, locationPath, locationUser, locationPass, locationPort, workspaceMode);
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
                        "[folder, shared (network location), SVN, TFS, Perforce, GIT] but was %s", locationType));
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
        scanConfig.setMaxZipSize(props.getIntProperty(KEY_MAX_ZIP_SIZE));
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
        scanConfig.setPaths(locationPath.split(";"));
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
        scanConfig.setPaths(locationPath.split(";"));
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
        scanConfig.setPaths(locationPath.split(";"));
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

    private static CxScanConfig setPerforceSourceLocation(CxScanConfig scanConfig, String locationURL, String locationPath, String locationUser, String locationPass, String locationPort, String workspaceMode) throws CLIParsingException {
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
        scanConfig.setPaths(locationPath.split(";"));
        scanConfig.setRemoteSrcUrl(locationURL);
        scanConfig.setRemoteType(RemoteSourceTypes.PERFORCE);
        scanConfig.setPerforceMode(workspaceMode);
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

    private static CxScanConfig setScanReports(CommandLine consoleParams, CxScanConfig scanConfig) throws URISyntaxException {
        if (consoleParams.getOptionValue(PDF_REPORT) != null) {
            String pdfReportPath = consoleParams.getOptionValue(PDF_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.addPDFReport(pdfReportPath);
        }
        if (consoleParams.getOptionValue(XML_REPORT) != null) {
            String xmlReportPath = consoleParams.getOptionValue(XML_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.addXMLReport(xmlReportPath);
        }
        if (consoleParams.getOptionValue(CSV_REPORT) != null) {
            String csvReportPath = consoleParams.getOptionValue(CSV_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.addCSVReport(csvReportPath);
        }
        if (consoleParams.getOptionValue(RTF_REPORT) != null) {
            String rtfReportPath = consoleParams.getOptionValue(RTF_REPORT).replace("..\\", "").replace("..//", "");
            scanConfig.addRTFReport(rtfReportPath);
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

    private static void setScanMode(Command mode, CommandLine cmd, CxScanConfig scanConfig) {
        scanConfig.setSastEnabled(mode.equals(Command.SCAN) || mode.equals(Command.ASYNC_SCAN));
        scanConfig.setSynchronous(mode.equals(Command.SCAN) || mode.equals(Command.OSA_SCAN));

        boolean dependencyScanEnabled = cmd.hasOption(OSA_ENABLED) ||
                cmd.hasOption(SCA_ENABLED) ||
                cmd.getOptionValue(OSA_LOCATION_PATH) != null ||
                mode.equals(Command.OSA_SCAN) ||
                mode.equals(Command.ASYNC_OSA_SCAN);

        if (dependencyScanEnabled){
            // If for some reason both SCA_ENABLED and OSA_ENABLED options are provided, SCA will take precedence.
            scanConfig.setDependencyScannerType(cmd.hasOption(SCA_ENABLED) ? DependencyScannerType.SCA : DependencyScannerType.OSA);
        }
        else {
            scanConfig.setDependencyScannerType(DependencyScannerType.NONE);
        }

        // TODO: GENERATE_TOKEN and REVOKE_TOKEN commands.
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

    public static void printConfig(CommandLine commandLine) {
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


}
