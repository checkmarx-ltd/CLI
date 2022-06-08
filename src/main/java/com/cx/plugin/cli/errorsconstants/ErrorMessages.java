package com.cx.plugin.cli.errorsconstants;

/**
 * Created by idanA on 11/28/2018.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String NO_ERROR_MSG = "Completed successfully";
    public static final String GENERAL_ERROR_MSG = "Failed to start scan (missing or invalid parameters)";
    public static final String SDLC_ERROR_MSG = "This feature is available only on SDLC edition";
    public static final String NO_OSA_LICENSE_ERROR_MSG = "Open Source Analysis License is not enabled for this project.Please contact your CxSAST Administrator";
    public static final String OSA_RESOLVE_ERROR_MSG = "Failed to resolve dependencies for OSA scan:";
    public static final String OSA_NO_DEPENDENCIES_ERROR_MSG = "No dependencies found for OSA scan";
    public static final String LOGIN_FAILED_MSG = "Login Failed";
    public static final String UNSUCCESSFUL_LOGIN_ERROR_MSG = "Unsuccessful login";
    public static final String UNSUCCESSFUL_REST_LOGIN = "Fail to login with credentials: Fail to authenticate: status code: HTTP/1.1 403 Forbidden.";
    public static final String INVALID_CREDENTIALS_FOR_TOKEN_GENERATION = "User authentication failed";
    public static final String INVALID_ACCESS_TOKEN = "Failed to generate access token from refresh token";
    public static final String INVALID_CREDENTIALS = "invalid_username_or_password";
    public static final String NO_PROJECT_PRIOR_TO_OSA_SCAN_ERROR_MSG = "OSA scan requires an existing project on the server";
    public static final String REPORT_PARAMETER_IN_ASYNC_SCAN = "Asynchronous run does not allow report creation. Please remove the report parameters and run again";
    public static final String THRESHOLD_PARAMETER_IN_ASYNC_SCAN = "Asynchronous run does not support threshold. Please remove the threshold parameters and run again";
    public static final String SERVER_CONNECTIVITY_VALIDATION_ERROR = "Failed to validate server connectivity: ";

    public static final String SAST_HIGH_THRESHOLD_ERROR_MSG = "SAST high vulnerability threshold exceeded";
    public static final String SAST_MEDIUM_THRESHOLD_ERROR_MSG = "SAST medium vulnerability threshold exceeded";
    public static final String SAST_LOW_THRESHOLD_ERROR_MSG = "SAST low vulnerability threshold exceeded";
    public static final String OSA_HIGH_THRESHOLD_ERROR_MSG = "OSA high vulnerability threshold exceeded";
    public static final String OSA_MEDIUM_THRESHOLD_ERROR_MSG = "OSA medium vulnerability threshold exceeded";
    public static final String OSA_LOW_THRESHOLD_ERROR_MSG = "OSA low vulnerability threshold exceeded";
    public static final String GENERIC_THRESHOLD_FAILURE_ERROR_MSG = "SAST and OSA vulnerabilities threshold exceeded";
    public static final String POLICY_VIOLATED_ERROR_MSG = "Policy violated";
    public static final String CANCELED_BY_USER_ERROR_MSG = "Scan canceled by user";
    public static final String INVALID_COMMAND = "[CxConsole] Malformed console command: Expected command to start with one of: %s but was: %s";
    public static final String MISSING_DASH = "[CxConsole] Malformed console command: parameter must start with '-' but was %s";
    public static final String INVALID_COMMAND_ERROR = "command [%s] is invalid, valid command must start with one of %s";
    public static final String INVALID_COMMAND_COUNT = "Invalid command count, One command must be provided Only. One of these commands must be provided only %s. ";

    public static final String OSA_MAVEN_RESOLVE_ERROR_MSG = "Failed to resolve Maven dependencies for OSA scan";
    public static final String OSA_GRADLE_RESOLVE_ERROR_MSG = "Failed to resolve Gradle dependencies for OSA scan";
    public static final String OSA_NPM_RESOLVE_ERROR_MSG = "Failed to resolve NPM dependencies for OSA scan";
    public static final String OSA_NUGET_RESOLVE_ERROR_MSG = "Failed to resolve Nuget/DotNet dependencies for OSA scan";

}
