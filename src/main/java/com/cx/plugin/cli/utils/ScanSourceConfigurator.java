package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.RemoteSourceTypes;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.KEY_MAX_ZIP_SIZE;

class ScanSourceConfigurator {
    private static final String SVN_DEFAULT_PORT = "80";
    private static final String PERFORCE_DEFAULT_PORT = "1666";
    private static final String TFS_DEFAULT_PORT = "8080";

    private String LOCATION_PATH_EXCEPTION = "[CxConsole] locationPath parameter needs to be specified when using [%s] as locationType";
    private String LOCATION_USER_EXCEPTION = "[CxConsole] locationUser parameter needs to be specified when using [%s] as locationType";
    private String LOCATION_PASSWORD_EXCEPTION = "[CxConsole] locationPassword parameter needs to be specified when using [%s] as locationType";
    private String LOCATION_URL_EXCEPTION = "[CxConsole] locationURL parameter needs to be specified when using [%s] as locationType";
    private String INVALID_PORT_EXCEPTION = "[CxConsole] Invalid port specified: [%s]";


    private static final Logger log = LogManager.getLogger(ScanSourceConfigurator.class);

    private CxScanConfig scanConfig;

    void configureSourceLocation(CommandLine commandLine, PropertiesManager props, CxScanConfig target)
            throws CLIParsingException {

        scanConfig = target;

        String locationType = commandLine.getOptionValue(LOCATION_TYPE);
        if (locationType == null) {
            throw new CLIParsingException("[CxConsole] No location type parameter was specified, needs to be specified using -LocationType <folder/shared/tfs/svn/perforce/git>");
        }

        final String locationPath = commandLine.getOptionValue(LOCATION_PATH);
        final String locationURL = commandLine.getOptionValue(LOCATION_URL);
        final String locationBranch = commandLine.getOptionValue(LOCATION_BRANCH);
        final String locationUser = commandLine.getOptionValue(LOCATION_USER);
        final String locationPass = commandLine.getOptionValue(LOCATION_PASSWORD);
        final String workspaceMode = commandLine.getOptionValue(WORKSPACE_MODE);

        String locationPort = commandLine.getOptionValue(LOCATION_PORT);

        switch (locationType.toLowerCase()) {
            case "folder": {
                setLocalSourceLocation(locationPath, props);
                break;
            }
            case "shared": {
                setSharedSourceLocation(locationPath, locationUser, locationPass);
                break;
            }
            case "svn": {
                if (commandLine.getOptionValue(PRIVATE_KEY) != null) {
                    setPrivateKey(commandLine.getOptionValue(PRIVATE_KEY));
                }
                setSVNSourceLocation(locationURL, locationPath, locationUser, locationPass, locationPort);
                break;
            }
            case "tfs": {
                setTFSSourceLocation(locationURL, locationPath, locationUser, locationPass, locationPort);
                break;
            }
            case "perforce": {
                setPerforceSourceLocation(locationURL, locationPath, locationUser, locationPass, locationPort, workspaceMode);
                break;
            }
            case "git": {
                if (commandLine.getOptionValue(PRIVATE_KEY) != null) {
                    setPrivateKey(commandLine.getOptionValue(PRIVATE_KEY));
                }
                setGITSourceLocation(locationURL, locationBranch, locationUser, locationPass, locationPort);
                break;
            }
            default: {
                throw new CLIParsingException(String.format("[CxConsole] location type must be one of " +
                        "[folder, shared (network location), SVN, TFS, Perforce, GIT] but was %s", locationType));
            }
        }
    }

    private void setLocalSourceLocation(String locationPath, PropertiesManager props) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format(LOCATION_PATH_EXCEPTION, "folder"));
        }
        scanConfig.setSourceDir(locationPath);
        scanConfig.setMaxZipSize(props.getIntProperty(KEY_MAX_ZIP_SIZE));        
    }

    private void setSharedSourceLocation(String locationPath, String locationUser, String locationPass) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format(LOCATION_PATH_EXCEPTION, RemoteSourceTypes.SHARED.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format(LOCATION_USER_EXCEPTION, RemoteSourceTypes.SHARED.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format(LOCATION_PASSWORD_EXCEPTION, RemoteSourceTypes.SHARED.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(locationPass);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setPaths(locationPath.split(";"));
        scanConfig.setRemoteType(RemoteSourceTypes.SHARED);
    }

    private void setSVNSourceLocation(String locationURL, String locationPath, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format(LOCATION_PATH_EXCEPTION, RemoteSourceTypes.SVN.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format(LOCATION_URL_EXCEPTION, RemoteSourceTypes.SVN.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(String.valueOf(locationPass));
        scanConfig.setRemoteType(RemoteSourceTypes.SVN);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setPaths(locationPath.split(";"));
        scanConfig.setRemoteSrcUrl(locationURL);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info("[CxConsole] No port was specified for SVN, using port {} as default", SVN_DEFAULT_PORT);
            locationPort = SVN_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format(INVALID_PORT_EXCEPTION, locationPort), e);
        }
    }

    private void setTFSSourceLocation(String locationURL, String locationPath, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format(LOCATION_PATH_EXCEPTION, RemoteSourceTypes.TFS.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format(LOCATION_URL_EXCEPTION, RemoteSourceTypes.TFS.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format(LOCATION_USER_EXCEPTION, RemoteSourceTypes.TFS.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format(LOCATION_PASSWORD_EXCEPTION, RemoteSourceTypes.TFS.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(locationPass);
        scanConfig.setRemoteType(RemoteSourceTypes.TFS);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setPaths(locationPath.split(";"));
        scanConfig.setRemoteSrcUrl(locationURL);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info("[CxConsole] No port was specified for TFS, using port {} as default", TFS_DEFAULT_PORT);
            locationPort = TFS_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format(INVALID_PORT_EXCEPTION, locationPort), e);
        }
    }

    private void setPerforceSourceLocation(String locationURL, String locationPath, String locationUser, String locationPass, String locationPort, String workspaceMode) throws CLIParsingException {
        if (Strings.isNullOrEmpty(locationPath)) {
            throw new CLIParsingException(String.format(LOCATION_PATH_EXCEPTION, RemoteSourceTypes.PERFORCE.value()));
        }
        if (Strings.isNullOrEmpty(locationURL)) {
            throw new CLIParsingException(String.format(LOCATION_URL_EXCEPTION, RemoteSourceTypes.PERFORCE.value()));
        }
        if (Strings.isNullOrEmpty(locationUser)) {
            throw new CLIParsingException(String.format(LOCATION_USER_EXCEPTION, RemoteSourceTypes.PERFORCE.value()));
        }
        if (locationPass == null) {
            throw new CLIParsingException(String.format(LOCATION_PASSWORD_EXCEPTION, RemoteSourceTypes.PERFORCE.value()));
        }

        scanConfig.setRemoteSrcUser(locationUser);
        scanConfig.setRemoteSrcPass(locationPass);
        scanConfig.setSourceDir(locationPath);
        scanConfig.setPaths(locationPath.split(";"));
        scanConfig.setRemoteSrcUrl(locationURL);
        scanConfig.setRemoteType(RemoteSourceTypes.PERFORCE);
        scanConfig.setPerforceMode(workspaceMode);
        if (Strings.isNullOrEmpty(locationPort)) {
            log.info("[CxConsole] No port was specified for perforce, using port {} as default", PERFORCE_DEFAULT_PORT);
            locationPort = PERFORCE_DEFAULT_PORT;
        }
        try {
            scanConfig.setRemoteSrcPort(Integer.parseInt(locationPort));
        } catch (NumberFormatException e) {
            throw new CLIParsingException(String.format(INVALID_PORT_EXCEPTION, locationPort), e);
        }
    }

    private void setGITSourceLocation(String locationURL, String locationBranch, String locationUser, String locationPass, String locationPort) throws CLIParsingException {
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
            throw new CLIParsingException(String.format(INVALID_PORT_EXCEPTION, locationPort), e);
        }
    }

    private void setPrivateKey(String pathToKey) throws CLIParsingException {
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
    }
}
