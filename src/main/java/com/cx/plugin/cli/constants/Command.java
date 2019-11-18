package com.cx.plugin.cli.constants;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.List;

import static com.cx.plugin.cli.constants.Parameters.*;

/**
 * Created by idanA on 11/26/2018.
 */
public enum Command {
    //TODO: add usages example for async scan
    SCAN("Scan", Constants.SCAN_USAGE, Constants.SCAN_DESC),
    ASYNC_SCAN("AsyncScan", Constants.SCAN_USAGE, Constants.SCAN_DESC),
    OSA_SCAN("OsaScan", Constants.OSA_USAGE, Constants.OSA_DESC),
    ASYNC_OSA_SCAN("AsyncOsaScan", Constants.OSA_USAGE, Constants.OSA_DESC),
    GENERATE_TOKEN("GenerateToken", Constants.TOKEN_GEN_USAGE, Constants.TOKEN_GEN_DESC),
    REVOKE_TOKEN("RevokeToken", Constants.TOKEN_REVOKE_USAGE, Constants.TOKEN_REVOKE_DESC);

    private final String usageExample;
    private final String description;
    private final String value;
    private static final List<String> commandsList = new ArrayList<>();

    static {
        for (Command command : Command.values()) {
            commandsList.add(command.value.toLowerCase());
        }
    }

    public static Command getCommandByValue(String value) {
        Command commandByValue = null;
        for (Command command : Command.values()) {
            if (command.value().equalsIgnoreCase(value)) {
                commandByValue = command;
                break;
            }
        }
        return commandByValue;
    }

    Command(String value, String usageExample, String description) {
        this.value = value;
        this.usageExample = usageExample;
        this.description = description;
    }

    @Override
    public String toString() {
        return value;
    }

    public static List<String> getAllValues() {
        return commandsList;
    }

    public String getUsageExamples() {
        return usageExample;
    }

    public String getDescription() {
        return description;
    }

    public String value() {
        return value;
    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption(SERVER_URL, true, Constants.CX_SERVER_DESC);
        options.addOption(USER_NAME, true, Constants.CX_USER_DESC);
        options.addOption(USER_PASSWORD, true, Constants.CX_PASS_DESC);
        options.addOption(TOKEN, true, Constants.CX_TOKEN_DESC);
        options.addOption(GENERATETOKEN, false, Constants.TOKEN_GEN_DESC);
        options.addOption(REVOKETOKEN, true, Constants.TOKEN_REVOKE_DESC);

        options.addOption(FULL_PROJECT_PATH, true, Constants.PROJECT_NAME_DESC);
        options.addOption(IS_CHECKED_POLICY, false, Constants.IS_CHECKED_POLICY_DESC);
        options.addOption(WORKSPACE_MODE, true, Constants.WORKSPACE_MODE_DESC);
        options.addOption(LOCATION_TYPE, true, Constants.LOCATION_TYPE_DESC);
        options.addOption(LOCATION_PATH, true, Constants.LOCATION_PATH_DESC);
        options.addOption(LOCATION_BRANCH, true, Constants.LOCATION_BRANCH_DESC);
        options.addOption(PRIVATE_KEY, true, Constants.LOCATION_PRIVATE_KEY_DESC);
        options.addOption(LOCATION_URL, true, Constants.LOCATION_URL_DESC);
        options.addOption(LOCATION_PORT, true, Constants.LOCATION_PORT_DESC);
        options.addOption(LOCATION_USER, true, Constants.LOCATION_USER_DESC);
        options.addOption(LOCATION_PASSWORD, true, Constants.LOCATION_PASSWORD_DESC);
        options.addOption(LOCATION_PATH_EXCLUDE, true, Constants.LOCATION_PATH_EXCLUDE_DESC);
        options.addOption(LOCATION_FILES_EXCLUDE, true, Constants.LOCATION_FILES_EXCLUDE_DESC);

        options.addOption(OSA_LOCATION_PATH, true, Constants.OSA_LOCATION_PATH_DESC);
        options.addOption(OSA_FILES_INCLUDE, true, Constants.OSA_FILES_INCLUDE_DESC);
        options.addOption(OSA_FILES_EXCLUDE, true, Constants.OSA_FILES_EXCLUDE_DESC);
        options.addOption(OSA_FOLDER_EXCLUDE, true, Constants.OSA_FOLDER_EXCLUDE_DESC);
        options.addOption(OSA_ARCHIVE_TO_EXTRACT, true, Constants.OSA_ARCHIVE_TO_EXTRACT_DESC);
        options.addOption(OSA_SCAN_DEPTH, true, Constants.OSA_SCAN_DEPTH_DESC);
        options.addOption(OSA_ENABLED, false, Constants.OSA_ENABLED_DESC);
        options.addOption(SCA_ENABLED, false, Constants.SCA_ENABLED_DESC);
        options.addOption(OSA_JSON_REPORT, true, Constants.OSA_JSON_REPORT_DESC);
        options.addOption(INSTALL_PACKAGE_MANAGER, false, Constants.INSTALL_PACKAGE_MANAGER_DESC);
//        options.addOption(DOCKER_IMAGE_PATTERN, true, Constants.DOCKER_IMAGE_PATTERN_DESC);
//        options.addOption(DOCKER_EXCLUDE, true, Constants.DOCKER_EXCLUDE_DESC);

        options.addOption(PDF_REPORT, true, Constants.PDF_REPORT_DESC);
        options.addOption(XML_REPORT, true, Constants.XML_REPORT_DESC);
        options.addOption(CSV_REPORT, true, Constants.CSV_REPORT_DESC);
        options.addOption(RTF_REPORT, true, Constants.RTF_REPORT_DESC);

        options.addOption(IS_INCREMENTAL, false, Constants.IS_INCREMENTAL_DESC);
        options.addOption(IS_FORCE_SCAN, false, Constants.IS_FORCE_SCAN_DESC);
        options.addOption(IS_PRIVATE, false, Constants.IS_PRIVATE_DESC);
        options.addOption(Option.builder(PRESET).desc(Constants.PRESET_DESC).hasArg(true).argName("preset").build());
        options.addOption(Option.builder(SCAN_COMMENT).desc(Constants.SCAN_COMMENT_DESC).hasArg(true).argName("text").build());
        options.addOption(Option.builder(IS_SSO).desc(Constants.IS_SSO_DESC).hasArg(false).build());
        options.addOption(SAST_HIGH, true, Constants.SAST_HIGH_DESC);
        options.addOption(SAST_MEDIUM, true, Constants.SAST_MEDIUM_DESC);
        options.addOption(SAST_LOW, true, Constants.SAST_LOW_DESC);
        options.addOption(OSA_HIGH, true, Constants.OSA_HIGH_DESC);
        options.addOption(OSA_MEDIUM, true, Constants.OSA_MEDIUM_DESC);
        options.addOption(OSA_LOW, true, Constants.OSA_LOW_DESC);

        options.addOption(VERBOSE, VERBOSE_LONG, false, Constants.VERBOSE_DESC);
        options.addOption(LOG_PATH, true, Constants.LOG_PATH_DESC);

        return options;
    }

    private static class Constants {
        static final String SCAN_USAGE = "\n\nCxConsole Scan -Projectname SP\\Cx\\Engine\\AST -CxServer http://localhost -cxuser admin@cx -cxpassword admin -locationtype folder -locationpath C:\\cx" +
                " -preset All -incremental -reportpdf a.pdf\nCxConsole Scan -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin@cx -cxpassword admin -locationtype tfs" +
                " -locationurl http://vsts2003:8080 -locationuser dm\\matys -locationpassword XYZ -preset default -reportxml a.xml -reportpdf b.pdf" +
                " -incremental -forcescan\nCxConsole Scan -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin@cx -cxpassword admin -locationtype share" +
                " -locationpath '\\\\storage\\path1;\\\\storage\\path2' -locationuser dm\\matys -locationpassword XYZ -preset \"Sans 25\" -reportxls a.xls -reportpdf b.pdf -private -verbose -log a.log\n" +
                " -LocationPathExclude test*, *log* -LocationFilesExclude web.config , *.class\n";
        static final String PROJECT_NAME_DESC = "A full absolute name of a project. " +
                "The full Project name includes the whole path to the project, including Server, service provider, company, and team. " +
                "Example:  -ProjectName \"CxServer\\SP\\Company\\Users\\bs java\" " +
                "If project with such a name doesn't exist in the system, new project will be created.";
        static final String SCAN_DESC = "\nThe \"Scan\" command allows to scan new and existing projects." +
                " It accepts all project settings as an arguments, similar to Web interface.";

        static final String OSA_DESC = "\nThe \"OsaScan\" command enables you to run an open source analysis (CxOSA) scan for an existing project as a CLI command. The CxOSA scan is, by default, run in synchronous mode (OsaScan)." +
                " This means that the CLI initiates the scan task and the scan results can be viewed in the CLI and in the log file created.";

        static final String CX_TOKEN_DESC = "Login token. Mandatory, Unless username and password are provided or SSO login is used on Windows ('-useSSO' flag)";
        static final String CX_PASS_DESC = "Login password. Mandatory, Unless token is used or SSO login is used on Windows ('-useSSO' flag)";
        static final String CX_USER_DESC = "Login username. Mandatory, Unless token is used or SSO login is used on Windows ('-useSSO' flag)";
        static final String CX_SERVER_DESC = "IP address or resolvable name of CxSuite web server";

        static final String WORKSPACE_MODE_DESC = "When -LocationType parameter is set to Perforce, add this parameter and add the workspace name into -locationPath. Optional";
        static final String LOCATION_TYPE_DESC = "Source location type, one of: folder,shared (network location), SVN, TFS, Perforce, GIT. Mandatory";

        static final String TOKEN_GEN_DESC = "\nThe \"GenerateToken\" command allows to generate login token, to be used instead of username and password.";
        static final String TOKEN_GEN_USAGE = "runCxConsole.cmd GenerateToken -CxServer http://localhost -cxuser admin@company -cxpassword admin -v";
        static final String TOKEN_REVOKE_DESC = "\nThe \"RevokeToken\" command allows to discard existing token.";
        static final String TOKEN_REVOKE_USAGE = "runCxConsole.cmd RevokeToken -CxToken 1241513513tsfrg42 -CxServer http://localhost -v";

        static final String OSA_USAGE = "\n\nrunCxConsole.cmd OsaScan -v -Projectname SP\\Cx\\Engine\\AST -CxServer http://localhost -cxuser admin -cxpassword admin -osaLocationPath C:\\cx  -OsaFilesExclude *.class OsaPathExclude src,temp  \n"
                + "runCxConsole.cmd  OsaScan -v -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin -cxpassword admin -locationtype folder -locationurl http://vsts2003:8080 -locationuser dm\\matys -locationpassword XYZ  \n"
                + "runCxConsole.cmd  OsaScan -v -projectname SP\\Cx\\Engine\\AST -cxserver http://localhost -cxuser admin -cxpassword admin -locationtype shared -locationpath '\\storage\\path1;\\storage\\path2' -locationuser dm\\matys -locationpassword XYZ  -log a.log\n \n"
                + "runCxConsole.cmd  OsaScan -v -Projectname CxServer\\SP\\Company\\my project -CxServer http://localhost -cxuser admin -cxpassword admin -locationtype folder -locationpath C:\\Users\\some_project -OsaFilesExclude *.bat ";
        static final String LOCATION_PATH_DESC = "Local or network path to sources or source repository branch. Mandatory if location type is folder, SVN, TFS, Perforce or shared.";
        static final String LOCATION_URL_DESC = "Source control URL. Madnatory if locationtype is any source control system.";
        static final String LOCATION_BRANCH_DESC = "Source GIT branch, Mandatory if location type is GIT";
        static final String LOCATION_PORT_DESC = "Source control system port. Default: 8080 (TFS), 80 (SVN), or 1666 (Perforce). Optional.";
        static final String LOCATION_USER_DESC = "Source control/network credentials. Mandatory if locationtype is TFS/Perforce/shared";
        static final String LOCATION_PASSWORD_DESC = "Source control/network credentials. Mandatory if locationtype is TFS/Perforce/shared";
        static final String LOCATION_PATH_EXCLUDE_DESC = "Comma separated list of folder name patterns to exclude from scan. For example, exclude all test and log folders: -locationPathExclude test* log_* Optional";
        static final String LOCATION_FILES_EXCLUDE_DESC = "Comma separated list of file name patterns to exclude from scan. For example, exclude all files with '.class' extension: -LocationFilesExclude *.class Optional";
        static final String LOCATION_PRIVATE_KEY_DESC = "GIT SSH key locations, Mandatory if location type is GIT using SSH";

        static final String OSA_LOCATION_PATH_DESC = "Local or network path to sources or source repository branch. May include multiple list of folders (local or shared) separated by comma. Optional";
        static final String OSA_FILES_INCLUDE_DESC = "Comma separated list of file name patterns to exclude from the OSA scan. \n" +
                "For example: *.dll will include only dll files. Optional";
        static final String OSA_FILES_EXCLUDE_DESC = "Comma separated list of file name patterns to exclude from the OSA scan. Exclude extensions by using *.<extension>, or exclude files by using */<file>.\n" +
                "Examples: -OsaFilesExclude *.class excludes all files which start with the .class. \n" +
                "Examples: -OsaFilesExclude */plexus-utils-1.5.6.jar excludes all files which start with plexus-utils-1.5.6.ja. Optional";
        static final String OSA_FOLDER_EXCLUDE_DESC = "Comma separated list of folder path patterns to exclude from the OSA scan. \n" +
                "For example: -OsaPathExclude test excludes all folders which start with test prefix. Optional.";
        static final String OSA_ARCHIVE_TO_EXTRACT_DESC = "Comma separated list of file extensions to be extracted in the OSA scan. \n" +
                "For example: -OsaArchiveToExtract *.zip extracts only files with .zip extension. Optional.";
        static final String OSA_SCAN_DEPTH_DESC = "Extraction depth of files to include in the OSA scan. Optional.";
        static final String OSA_ENABLED_DESC = "Enable open source analysis (CxOSA). -osaLocationPath should be specified or the -LocationType parameter needs to be defined as 'folder' or 'shared' (if -osaLocationPath doesn't exist, use -locationPath). Optional.";
        static final String SCA_ENABLED_DESC = String.format("Enable software composition analysis (SCA). SCA is the successor of CxOSA. Normally either -%1$s or -%2$s should be specified. If both are specified, -%2$s will be used. -osaLocationPath should be specified or the -LocationType parameter needs to be defined as 'folder' or 'shared' (if -osaLocationPath doesn't exist, use -locationPath). Optional.", OSA_ENABLED, SCA_ENABLED);
        static final String OSA_JSON_REPORT_DESC = "Generate CxOSA JSON report. Optional, not supported in AsyncScan mode";
        static final String INSTALL_PACKAGE_MANAGER_DESC = "Retrieve all supported package dependencies before performing OSA scan (see Remarks section). Optional.";
        static final String DOCKER_IMAGE_PATTERN_DESC = "The docker images to be selected for scan"; //TODO: add the description
        static final String DOCKER_EXCLUDE_DESC = "Set the GLOB pattern property for excluding docker files to scan. Optional.";
        static final String PDF_REPORT_DESC = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
        static final String XML_REPORT_DESC = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
        static final String CSV_REPORT_DESC = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
        static final String RTF_REPORT_DESC = "Name or path to results report, by type. Optional. Not supported in AsyncScan mode.";
        static final String IS_INCREMENTAL_DESC = "Run incremental scan instead of a full scan. Scans only new and modified files, relative to project's last scan(-Incremental will disable any -ForceScan setting). Optional.";
        static final String IS_FORCE_SCAN_DESC = "Force scan on source code, which has not been changed since the last scan of the same project (not compatible with -Incremental option). Optional.";
        static final String IS_PRIVATE_DESC = "Scan will not be visible to other users. Optional";
        static final String PRESET_DESC = "If not provided, will use preset defined in existing project or, for a new project, the default preset. Optional";
        static final String SCAN_COMMENT_DESC = "Saves a comment with the scan results. For example: -comment 'important scan1'. Optional. Not supported in AsyncScan mode";
        static final String IS_SSO_DESC = "Single Sign-On: Use Windows credentials of current user to log into CxSAST. Optioanl";
        static final String SAST_HIGH_DESC = "CxSAST high severity vulnerability threshold. If the number of high vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String SAST_MEDIUM_DESC = "CxSAST medium severity vulnerability threshold. If the number of medium vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String SAST_LOW_DESC = "CxSAST low severity vulnerability threshold. If the number of low vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String OSA_HIGH_DESC = "CxOSA high severity vulnerability threshold. If the number of high vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String OSA_MEDIUM_DESC = "CxOSA medium severity vulnerability threshold. If the number of medium vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String OSA_LOW_DESC = "CxOSA low severity vulnerability threshold. If the number of low vulnerabilities exceeds the threshold, scan will end with an error (see Error/Exit Codes). Optional. Not supported in AsyncScan mode";
        static final String IS_CHECKED_POLICY_DESC = "This parameter will break the build if the CxOSA policy is violated. Optional.";
        static final String VERBOSE_DESC = "Turns on verbose mode. All messages and events will be sent to the console or log file.";
        static final String LOG_PATH_DESC = "Log file to be created.";
    }
}
