package com.cx.plugin.cli.constants;

/**
 * Created by idanA on 11/27/2018.
 */
public final class Parameters {


    private Parameters() {
        throw new IllegalStateException("Utility class");
    }

    public static final String VERBOSE = "v";
    public static final String VERBOSE_LONG = "verbose";
    public static final String CLI_CONFIG = "config";
    public static final String SERVER_URL = "cxserver";
    public static final String USER_NAME = "cxuser";
    public static final String USER_PASSWORD = "cxpassword";
    public static final String GENERATETOKEN = "generatetoken";
    public static final String REVOKETOKEN = "revoketoken";
    public static final String TOKEN = "cxtoken";

    public static final String FULL_PROJECT_PATH = "projectname";
    public static final String LOG_PATH = "log";
    public static final String IS_CHECKED_POLICY = "checkpolicy";

    public static final String WORKSPACE_MODE = "workspacemode";
    public static final String LOCATION_TYPE = "locationtype";
    public static final String LOCATION_PATH = "locationpath";
    public static final String LOCATION_URL = "locationurl";
    public static final String LOCATION_PORT = "locationport";
    public static final String LOCATION_BRANCH = "locationbranch";
    public static final String LOCATION_USER = "locationuser";
    public static final String LOCATION_PASSWORD = "locationpassword";
    public static final String LOCATION_PATH_EXCLUDE = "locationpathexclude";
    public static final String LOCATION_FILES_EXCLUDE = "locationfilesexclude";
    public static final String PRIVATE_KEY = "locationprivatekey";
    public static final String OSA_LOCATION_PATH = "osalocationpath";
    public static final String OSA_FILES_INCLUDE = "osafilesinclude";
    public static final String OSA_FILES_EXCLUDE = "osafilesexclude";
    public static final String OSA_FOLDER_EXCLUDE = "osapathexclude";
    public static final String INCLUDE_EXCLUDE_PATTERN = "includeexcludepattern";
    public static final String OSA_ARCHIVE_TO_EXTRACT = "osaarchivetoextract";
    public static final String OSA_SCAN_DEPTH = "osascandepth";
    public static final String OSA_ENABLED = "enableosa";
    public static final String SCA_ENABLED = "enablesca";
    public static final String OSA_JSON_REPORT = "osajson";
    public static final String SCA_JSON_REPORT = "scajsondirpath";
    public static final String INSTALL_PACKAGE_MANAGER = "executepackagedependency";
    public static final String DOCKER_IMAGE_PATTERN = "dockerscan";
    public static final String DOCKER_EXCLUDE = "dockerexcludescan";

    public static final String CX_ORIGIN = "cx-CLI";
    public static final String PDF_REPORT = "reportpdf";
    public static final String XML_REPORT = "reportxml";
    public static final String CSV_REPORT = "reportcsv";
    public static final String RTF_REPORT = "reportrtf";
    public static final String IS_INCREMENTAL = "incremental";
    public static final String IS_FORCE_SCAN = "forcescan";
    public static final String IS_PRIVATE = "private";

    public static final String PRESET = "preset";
    public static final String SCAN_COMMENT = "comment";
    public static final String IS_SSO = "usesso";

    public static final String SAST_HIGH = "sasthigh";
    public static final String SAST_MEDIUM = "sastmedium";
    public static final String SAST_LOW = "sastlow";

    public static final String OSA_HIGH = "osahigh";
    public static final String OSA_MEDIUM = "osamedium";
    public static final String OSA_LOW = "osalow";
    public static final String TRUSTED_CERTIFICATES = "trustedcertificates";

    public static final String SCA_API_URL = "scaapiurl";
    public static final String SCA_ACCESS_CONTROL_URL = "scaaccesscontrolurl";
    public static final String SCA_WEB_APP_URL = "scawebappurl";
    public static final String SCA_USERNAME = "scausername";
    public static final String SCA_PASSWORD = "scapassword";
    public static final String SCA_ACCOUNT = "scaaccount";

    public static final String SCA_HIGH = "scahigh";
    public static final String SCA_MEDIUM = "scamedium";
    public static final String SCA_LOW = "scalow";
    public static final String SCA_FILES_INCLUDE = "scafilesinclude";
    public static final String SCA_FILES_EXCLUDE = "scafilesexclude";
    public static final String SCA_FOLDER_EXCLUDE = "scapathexclude";
    public static final String SCA_LOCATION_PATH = "scalocationpath";
    public static final String CONFIG_AS_CODE = "configascode";
    public static final String CONFIGURATION = "configuration";

    public static final String NTLM = "ntlm";

    public static final String ENV_VARIABLE = "env";
    public static final String SAST_PROJECT_ID = "cxprojectid";
    public static final String SCA_CONFIG_FILE = "scaconfigfile";

}
