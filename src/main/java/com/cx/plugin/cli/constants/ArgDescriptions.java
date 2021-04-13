package com.cx.plugin.cli.constants;

class ArgDescriptions {

    private ArgDescriptions() {
        throw new IllegalStateException("Utility class");
    }

    private static final String SYNC_SCAN_NOTE = "The scan is, by default, run in synchronous mode. This means that the CLI initiates the scan task and the scan results can be viewed in the CLI and in the log file created.";

    private static final String THRESHOLD_TEMPLATE = "%1$s %2$s severity vulnerability threshold. If the number of %2$s vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in asynchronous mode.";

    private static final String FILE_INCLUDE_TEMPLATE = "Comma separated list of file name patterns to include to the %s scan. For example: *.dll will include only dll files. Optional";

    private static final String FILE_EXCLUDE_TEMPLATE = "Comma separated list of file name patterns to exclude from the %1$s scan. Exclude extensions by using !*.<extension>, or exclude files by using !*/<file>.\n" +
            "Examples: -%2$s !*.class excludes all files which start with the .class. \n" +
            "Examples: -%2$s !*/plexus-utils-1.5.6.jar excludes all files which start with plexus-utils-1.5.6.jar. Optional";

    private static final String FOLDER_EXCLUDE_TEMPLATE = "Comma separated list of folder path patterns to exclude from the %s scan. \n" +
            "For example: -%s test excludes all folders which start with test prefix. Optional.";

    static final String PROJECT_NAME = "A full absolute name of a project. " +
            "The full Project name includes the whole path to the project, including Server, service provider, company, and team. " +
            "Example:  -ProjectName \"CxServer\\SP\\Company\\Users\\bs java\" " +
            "If project with such a name doesn't exist in the system, new project will be created.";
    static final String SCAN = "\nThe \"Scan\" command allows to scan new and existing projects." +
            " It accepts all project settings as an arguments, similar to Web interface.";

    static final String OSA = "\nThe \"OsaScan\" command enables you to run a dependency scan for an existing project using the legacy CxOSA engine. " +
            SYNC_SCAN_NOTE;

    static final String SCA_SCAN = "This command enables you to run a dependency scan for an existing project using the new CxSCA engine. " +
            SYNC_SCAN_NOTE;

    static final String ASYNC_SCA_SCAN = "This command enables you to run a dependency scan for an existing project using the new CxSCA engine. The scan is run in asynchronous mode. This means that the scan task ends when the scan request reaches the scan queue. As a result the scan results can only be viewed via the CxSCA web application.";

    static final String CX_TOKEN = "Login token. Mandatory, Unless username and password are provided or SSO login is used on Windows ('-useSSO' flag)";
    static final String CX_PASS = "Login password. Mandatory, Unless token is used or SSO login is used on Windows ('-useSSO' flag)";
    static final String CX_USER = "Login username. Mandatory, Unless token is used or SSO login is used on Windows ('-useSSO' flag)";
    static final String CX_SERVER = "IP address or resolvable name of CxSuite web server";
    static final String CX_CLI_CONFIG = "Path to CLI configuration file";

    static final String WORKSPACE_MODE = "When -LocationType parameter is set to Perforce, add this parameter and add the workspace name into -locationPath. Optional";
    static final String LOCATION_TYPE = "Source location type, one of: folder,shared (network location), SVN, TFS, Perforce, GIT. Mandatory";

    static final String TOKEN_GEN = "\nThe \"GenerateToken\" command allows to generate login token, to be used instead of username and password.";
    static final String TOKEN_REVOKE = "\nThe \"RevokeToken\" command allows to discard existing token.";

    static final String LOCATION_PATH = "Local or network path to sources or source repository branch. Mandatory if location type is folder, SVN, TFS, Perforce or shared.";
    static final String LOCATION_URL = "Source control URL. Madnatory if locationtype is any source control system.";
    static final String LOCATION_BRANCH = "Source GIT branch, Mandatory if location type is GIT";
    static final String LOCATION_PORT = "Source control system port. Default: 8080 (TFS), 80 (SVN), or 1666 (Perforce). Optional.";
    static final String LOCATION_USER = "Source control/network credentials. Mandatory if locationtype is TFS/Perforce/shared";
    static final String LOCATION_PASSWORD = "Source control/network credentials. Mandatory if locationtype is TFS/Perforce/shared";
    static final String LOCATION_PATH_EXCLUDE = "Comma separated list of folder name patterns to exclude from scan. For example, exclude all test and log folders: -locationPathExclude test* log_* Optional";
    static final String LOCATION_FILES_EXCLUDE = "Comma separated list of file name patterns to exclude from scan. For example, exclude all files with '.class' extension: -LocationFilesExclude *.class Optional";
    static final String INCLUDE_EXCLUDE_PATTERN = "Comma separated list of file name patterns to exclude/include from/to scan. For example, exclude all files with '.class' extension: -includeexcludepattern \"!*.class\". Optional";
    static final String LOCATION_PRIVATE_KEY = "GIT SSH key locations, Mandatory if location type is GIT using SSH";
    public static final String CONFIG_AS_CODE = "Override scan settings with Remote/Local configuration file located inside source directory";
    static final String CX_SAST = "CxSAST";
    static final String CX_OSA = "CxOSA";
    static final String CX_SCA = "CxSCA";

    static final String OSA_LOCATION_PATH = "Local or network path to sources or source repository branch. May include multiple list of folders (local or shared) separated by comma. Optional";
    static final String OSA_FILES_INCLUDE = String.format(FILE_INCLUDE_TEMPLATE, CX_OSA);
    static final String OSA_FILES_EXCLUDE = String.format(FILE_EXCLUDE_TEMPLATE, CX_OSA, "OsaFilesExclude");
    static final String OSA_FOLDER_EXCLUDE = String.format(FOLDER_EXCLUDE_TEMPLATE, CX_OSA, "OsaPathExclude");

    static final String OSA_ARCHIVE_TO_EXTRACT = "Comma separated list of file extensions to be extracted in the OSA scan. \n" +
            "For example: -OsaArchiveToExtract *.zip extracts only files with .zip extension. Optional.";
    static final String OSA_SCAN_DEPTH = "Extraction depth of files to include in the OSA scan. Optional.";
    static final String OSA_ENABLED = "Enable open source analysis (CxOSA). -osaLocationPath should be specified or the -LocationType parameter needs to be defined as 'folder' or 'shared' (if -osaLocationPath doesn't exist, use -locationPath). Optional.";
    static final String SCA_ENABLED = String.format("Enable software composition analysis (SCA). SCA is the successor of CxOSA. Normally either -%1$s or -%2$s should be specified. If both are specified, -%2$s will be used. -osaLocationPath should be specified or the -LocationType parameter needs to be defined as 'folder' or 'shared' (if -osaLocationPath doesn't exist, use -locationPath). Optional.", Parameters.OSA_ENABLED, Parameters.SCA_ENABLED);
    static final String OSA_JSON_REPORT = "Generate CxOSA JSON report. Optional, not supported in AsyncScan mode";
    static final String SCA_JSON_REPORT = "Generates three CxSCA JSON reports. Saves the reports in the specified folder path, Optional. Not supported in AsyncScaScan/AsyncScan mode";
    static final String INSTALL_PACKAGE_MANAGER = "Retrieve all supported package dependencies before performing OSA scan (see Remarks section). Optional.";
    static final String DOCKER_IMAGE_PATTERN = "The docker images to be selected for scan";
    static final String DOCKER_EXCLUDE = "Set the GLOB pattern property for excluding docker files to scan. Optional.";
    static final String PDF_REPORT = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
    static final String XML_REPORT = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
    static final String CSV_REPORT = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
    static final String RTF_REPORT = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
    static final String IS_INCREMENTAL = "Run incremental scan instead of a full scan. Scans only new and modified files, relative to project's last scan(-Incremental will disable any -ForceScan setting). Optional.";
    static final String IS_FORCE_SCAN = "Force scan on source code, which has not been changed since the last scan of the same project (not compatible with -Incremental option). Optional.";
    static final String IS_PRIVATE = "Scan will not be visible to other users. Optional";
    static final String PRESET = "If not provided, will use preset defined in existing project or, for a new project, the default preset. Optional";
    static final String SCAN_COMMENT = "Saves a comment with the scan results. For example: -comment 'important scan1'. Optional. Not supported in AsyncScan mode";
    static final String IS_SSO = "Single Sign-On: Use Windows credentials of current user to log into CxSAST. Optional.";

    static final String HIGH = "high";
    static final String MEDIUM = "medium";
    static final String LOW = "low";

    static final String SAST_HIGH = String.format(THRESHOLD_TEMPLATE, CX_SAST, HIGH);
    static final String SAST_MEDIUM = String.format(THRESHOLD_TEMPLATE, CX_SAST, MEDIUM);
    static final String SAST_LOW = String.format(THRESHOLD_TEMPLATE, CX_SAST, LOW);
    static final String OSA_HIGH = String.format(THRESHOLD_TEMPLATE, CX_OSA, HIGH);
    static final String OSA_MEDIUM = String.format(THRESHOLD_TEMPLATE, CX_OSA, MEDIUM);
    static final String OSA_LOW = String.format(THRESHOLD_TEMPLATE, CX_OSA, LOW);
    static final String IS_CHECKED_POLICY = "This parameter will break the build if the CxOSA policy is violated. Optional.";
    static final String VERBOSE = "Turns on verbose mode. All messages and events will be sent to the console or log file.";
    static final String LOG_PATH = "Log file to be created.";
    static final String TRUSTED_CERTIFICATES = "The ‘TrustedCertificates’ parameter can be used to add certified security to the connection. By default, all certificates are trusted. When disabled, only certificates signed by a trusted certificate authority can be accepted.";

    static final String SCA_API_URL = "IP address or resolvable name of a SCA instance.";
    static final String SCA_ACCESS_CONTROL_URL = "IP address or resolvable name of an access control server that is used to access SCA.";
    static final String SCA_WEB_APP_URL = "URL of the SCA web application. Used to generate web report URL. If this option is not provided in the command line, a value from the config file is used.";
    static final String SCA_USERNAME = "SCA username.";
    static final String SCA_PASSWORD = "SCA password.";
    static final String SCA_ACCOUNT = "Account name to be used during authentication.";

    static final String SCA_HIGH = String.format(THRESHOLD_TEMPLATE, CX_SCA, HIGH);
    static final String SCA_MEDIUM = String.format(THRESHOLD_TEMPLATE, CX_SCA, MEDIUM);
    static final String SCA_LOW = String.format(THRESHOLD_TEMPLATE, CX_SCA, LOW);
    static final String SCA_FILES_INCLUDE = String.format(FILE_INCLUDE_TEMPLATE, CX_SCA);
    static final String SCA_FILES_EXCLUDE = String.format(FILE_EXCLUDE_TEMPLATE, CX_SCA, "ScaFilesExclude");
    static final String SCA_LOCATION_PATH = "Local or network path to sources that should scanned. Optional.";
    static final String SCA_FOLDER_EXCLUDE = String.format(FOLDER_EXCLUDE_TEMPLATE, CX_SCA, "ScaPathExclude");

    static final String CONFIGURATION = "Code language configuration. Possible values are : Default Configuration, Japanese (Shift-JIS), Korean and Multi-language Scan. If configuration is not set, 'Default Configuration' is used.";

    static final String TEST_CONNECTION = "\nThe \"TestConnection\" command checks if login was successful or not.";

    static final String NTLM = "Use NTLM for proxy authentication";
    static final String ENV_VARIABLE = "Optional Environment Variables that could be used during the SCA scan process.";
    static final String SAST_PROJECT_ID = "Project Id from Checkmarx SAST used during SCA scan process.";
    static final String SCA_CONFIG_FILE = "Configuration files from package managers (maven,npm etc) needed for the SCA scan.";
}
