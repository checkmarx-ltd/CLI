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
    SCAN("Scan", UsageExamples.SCAN, ArgDescriptions.SCAN),
    ASYNC_SCAN("AsyncScan", UsageExamples.SCAN, ArgDescriptions.SCAN),

    OSA_SCAN("OsaScan", UsageExamples.OSA, ArgDescriptions.OSA),
    ASYNC_OSA_SCAN("AsyncOsaScan", UsageExamples.OSA, ArgDescriptions.OSA),

    SCA_SCAN("ScaScan", UsageExamples.SCA_SCAN, ArgDescriptions.SCA_SCAN),
    ASYNC_SCA_SCAN("AsyncScaScan", UsageExamples.ASYNC_SCA_SCAN, ArgDescriptions.ASYNC_SCA_SCAN),

    GENERATE_TOKEN("GenerateToken", UsageExamples.TOKEN_GEN, ArgDescriptions.TOKEN_GEN),
    REVOKE_TOKEN("RevokeToken", UsageExamples.TOKEN_REVOKE, ArgDescriptions.TOKEN_REVOKE),

    TEST_CONNECTION("TestConnection", UsageExamples.TEST_CONNECTION, ArgDescriptions.TEST_CONNECTION);

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
        options.addOption(CLI_CONFIG, true, ArgDescriptions.CX_CLI_CONFIG);
        options.addOption(SERVER_URL, true, ArgDescriptions.CX_SERVER);
        options.addOption(USER_NAME, true, ArgDescriptions.CX_USER);
        options.addOption(USER_PASSWORD, true, ArgDescriptions.CX_PASS);
        options.addOption(CUSTOM_FIELDS, true, ArgDescriptions.SCAN_LEVEL_CUSTOM_FIELDS);
        options.addOption(TOKEN, true, ArgDescriptions.CX_TOKEN);
        options.addOption(GENERATETOKEN, false, ArgDescriptions.TOKEN_GEN);
        options.addOption(REVOKETOKEN, true, ArgDescriptions.TOKEN_REVOKE);

        options.addOption(FULL_PROJECT_PATH, true, ArgDescriptions.PROJECT_NAME);
        options.addOption(IS_CHECKED_POLICY, false, ArgDescriptions.IS_CHECKED_POLICY);
        options.addOption(WORKSPACE_MODE, true, ArgDescriptions.WORKSPACE_MODE);
        options.addOption(LOCATION_TYPE, true, ArgDescriptions.LOCATION_TYPE);
        options.addOption(LOCATION_PATH, true, ArgDescriptions.LOCATION_PATH);
        options.addOption(LOCATION_BRANCH, true, ArgDescriptions.LOCATION_BRANCH);
        options.addOption(PRIVATE_KEY, true, ArgDescriptions.LOCATION_PRIVATE_KEY);
        options.addOption(LOCATION_URL, true, ArgDescriptions.LOCATION_URL);
        options.addOption(LOCATION_PORT, true, ArgDescriptions.LOCATION_PORT);
        options.addOption(LOCATION_USER, true, ArgDescriptions.LOCATION_USER);
        options.addOption(LOCATION_PASSWORD, true, ArgDescriptions.LOCATION_PASSWORD);
        options.addOption(LOCATION_PATH_EXCLUDE, true, ArgDescriptions.LOCATION_PATH_EXCLUDE);
        options.addOption(LOCATION_FILES_EXCLUDE, true, ArgDescriptions.LOCATION_FILES_EXCLUDE);
        options.addOption(INCLUDE_EXCLUDE_PATTERN, true, ArgDescriptions.INCLUDE_EXCLUDE_PATTERN);

        options.addOption(OSA_LOCATION_PATH, true, ArgDescriptions.OSA_LOCATION_PATH);
        options.addOption(OSA_FILES_INCLUDE, true, ArgDescriptions.OSA_FILES_INCLUDE);
        options.addOption(OSA_FILES_EXCLUDE, true, ArgDescriptions.OSA_FILES_EXCLUDE);
        options.addOption(OSA_FOLDER_EXCLUDE, true, ArgDescriptions.OSA_FOLDER_EXCLUDE);
        options.addOption(OSA_ARCHIVE_TO_EXTRACT, true, ArgDescriptions.OSA_ARCHIVE_TO_EXTRACT);
        options.addOption(OSA_SCAN_DEPTH, true, ArgDescriptions.OSA_SCAN_DEPTH);
        options.addOption(OSA_ENABLED, false, ArgDescriptions.OSA_ENABLED);
        options.addOption(SCA_ENABLED, false, ArgDescriptions.SCA_ENABLED);
        options.addOption(OSA_JSON_REPORT, true, ArgDescriptions.OSA_JSON_REPORT);
        options.addOption(SCA_JSON_REPORT, true, ArgDescriptions.SCA_JSON_REPORT);
        options.addOption(INSTALL_PACKAGE_MANAGER, false, ArgDescriptions.INSTALL_PACKAGE_MANAGER);
        options.addOption(OSA_FAIL_ON_ERROR, false, ArgDescriptions.OSA_FAIL_ON_ERROR);
        options.addOption(Option.builder(OSA_FSA_CONF).hasArg(true).hasArgs().argName("fsa configuration").desc(ArgDescriptions.OSA_FSA_CONF).valueSeparator(',').build());
        options.addOption(Option.builder(OSA_ERR_LOG_DIR).hasArg(true).hasArgs().argName("osa error log dir").desc(ArgDescriptions.OSA_ERR_LOG_DIR).build());
        options.addOption(Option.builder(OSA_SCAN_JSON).hasArg(true).hasArgs().argName("osa scan json").desc(ArgDescriptions.OSA_SCAN_JSON).build());

        options.addOption(PDF_REPORT, true, ArgDescriptions.PDF_REPORT);
        options.addOption(XML_REPORT, true, ArgDescriptions.XML_REPORT);
        options.addOption(CSV_REPORT, true, ArgDescriptions.CSV_REPORT);
        options.addOption(RTF_REPORT, true, ArgDescriptions.RTF_REPORT);

        
        options.addOption(GENERATE_SCA_REPORT, false, ArgDescriptions.GENERATE_SCA_REPORT);
        options.addOption(SCA_REPORT_FORMAT, true, ArgDescriptions.SCA_REPORT_FORMAT);
        options.addOption(SCA_REPORT_PATH, true, ArgDescriptions.SCA_REPORT_PATH);
        
        options.addOption(IS_INCREMENTAL, false, ArgDescriptions.IS_INCREMENTAL);
        options.addOption(IS_FORCE_SCAN, false, ArgDescriptions.IS_FORCE_SCAN);
        options.addOption(IS_PRIVATE, false, ArgDescriptions.IS_PRIVATE);
        options.addOption(Option.builder(PRESET).desc(ArgDescriptions.PRESET).hasArg(true).argName("preset").build());
        options.addOption(Option.builder(SCAN_COMMENT).desc(ArgDescriptions.SCAN_COMMENT).hasArg(true).argName("text").build());
        options.addOption(Option.builder(IS_SSO).desc(ArgDescriptions.IS_SSO).hasArg(false).build());
        options.addOption(SAST_CRITICAL, true, ArgDescriptions.SAST_CRITICAL);
        options.addOption(SAST_HIGH, true, ArgDescriptions.SAST_HIGH);
        options.addOption(SAST_MEDIUM, true, ArgDescriptions.SAST_MEDIUM);
        options.addOption(SAST_LOW, true, ArgDescriptions.SAST_LOW);
        options.addOption(OSA_HIGH, true, ArgDescriptions.OSA_HIGH);
        options.addOption(OSA_MEDIUM, true, ArgDescriptions.OSA_MEDIUM);
        options.addOption(OSA_LOW, true, ArgDescriptions.OSA_LOW);

        options.addOption(SCA_API_URL, true, ArgDescriptions.SCA_API_URL);
        options.addOption(SCA_ACCESS_CONTROL_URL, true, ArgDescriptions.SCA_ACCESS_CONTROL_URL);
        options.addOption(SCA_WEB_APP_URL, true, ArgDescriptions.SCA_WEB_APP_URL);
        options.addOption(SCA_USERNAME, true, ArgDescriptions.SCA_USERNAME);
        options.addOption(SCA_PASSWORD, true, ArgDescriptions.SCA_PASSWORD);
        options.addOption(SCA_ACCOUNT, true, ArgDescriptions.SCA_ACCOUNT);
		
		options.addOption(ENABLE_SCA_RESOLVER, false, ArgDescriptions.ENABLE_SCA_RESOLVER);//use SCA resolver
        options.addOption(PATH_TO_RESOLVER, true, ArgDescriptions.PATH_TO_RESOLVER);//path to resolver
        options.addOption(SCA_RESOLVER_ADD_PARAMETERS, true, ArgDescriptions.SCA_RESOLVER_ADD_PARAMETERS);//path to resolver

        options.addOption(SCA_CRITICAL, true, ArgDescriptions.SCA_CRITICAL);
        options.addOption(SCA_HIGH, true, ArgDescriptions.SCA_HIGH);
        options.addOption(SCA_MEDIUM, true, ArgDescriptions.SCA_MEDIUM);
        options.addOption(SCA_LOW, true, ArgDescriptions.SCA_LOW);
        options.addOption(SCA_FILES_INCLUDE, true, ArgDescriptions.SCA_FILES_INCLUDE);
        options.addOption(SCA_FILES_EXCLUDE, true, ArgDescriptions.SCA_FILES_EXCLUDE);
        options.addOption(SCA_LOCATION_PATH, true, ArgDescriptions.SCA_LOCATION_PATH);
        options.addOption(CONFIG_AS_CODE, false, ArgDescriptions.CONFIG_AS_CODE);
        options.addOption(SCA_FOLDER_EXCLUDE, true, ArgDescriptions.SCA_FOLDER_EXCLUDE);
        options.addOption(VERBOSE, VERBOSE_LONG, false, ArgDescriptions.VERBOSE);
        options.addOption(LOG_PATH, true, ArgDescriptions.LOG_PATH);
        options.addOption(LOG_LEVEL, true, ArgDescriptions.LOG_LEVEL);
        options.addOption(TRUSTED_CERTIFICATES, false, ArgDescriptions.TRUSTED_CERTIFICATES);
        options.addOption(CONFIGURATION, true, ArgDescriptions.CONFIGURATION);

        options.addOption(NTLM, false, ArgDescriptions.NTLM);

        options.addOption(ENV_VARIABLE, true, ArgDescriptions.ENV_VARIABLE);
        options.addOption(SAST_PROJECT_ID, true, ArgDescriptions.SAST_PROJECT_ID);
        options.addOption(SAST_PROJECT_NAME, true, ArgDescriptions.SAST_PROJECT_NAME);
        options.addOption(SAST_SERVER_URL, true, ArgDescriptions.SAST_SERVER_URL);
        options.addOption(SAST_USER, true, ArgDescriptions.SAST_USER);
        options.addOption(SAST_PASSWORD, true, ArgDescriptions.SAST_PASSWORD);

        options.addOption(SCA_CONFIG_FILE, true, ArgDescriptions.SCA_CONFIG_FILE);
        options.addOption(SCA_INCLUDE_SOURCE_FLAG, false, ArgDescriptions.SCA_INCLUDE_SOURCE_FLAG);
        options.addOption(SCA_TIMEOUT, true, ArgDescriptions.SCA_TIME_OUT);

        options.addOption(ENABLE_SAST_BRANCHING, false, ArgDescriptions.ENABLE_SAST_BRANCHING);
        options.addOption(MASTER_BRANCH_PROJ_NAME, true, ArgDescriptions.MASTER_BRANCH_PROJ_NAME);
        
        options.addOption(POST_SCAN_ACTION, true, ArgDescriptions.POST_SCAN_ACTION);

        options.addOption(PERIODIC_FULL_SCAN, true, ArgDescriptions.PERIODIC_FULL_SCAN);
        options.addOption(AVOID_DUPLICATE_PROJECT_SCANS, false, ArgDescriptions.AVOID_DUPLICATE_PROJECT_SCANS);
        options.addOption(BRANCH_TIMEOUT, true, ArgDescriptions.BRANCH_TIMEOUT);
        
        return options;
    }

}
