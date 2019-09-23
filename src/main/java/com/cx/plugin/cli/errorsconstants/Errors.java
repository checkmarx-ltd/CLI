package com.cx.plugin.cli.errorsconstants;


import static com.cx.plugin.cli.errorsconstants.ErrorMessages.*;

/**
 * Created by idanA on 11/5/2018.
 */
public enum Errors {
    SCAN_SUCCEEDED(0, NO_ERROR_MSG),
    GENERAL_ERROR(1, GENERAL_ERROR_MSG),
    SDLC_ERROR(2, SDLC_ERROR_MSG),
    NO_OSA_LICENSE_ERROR(3, NO_OSA_LICENSE_ERROR_MSG),
    LOGIN_FAILED_ERROR(4, LOGIN_FAILED_MSG),

    SAST_HIGH_THRESHOLD_ERROR(10, SAST_HIGH_THRESHOLD_ERROR_MSG),
    SAST_MEDIUM_THRESHOLD_ERROR(11, SAST_MEDIUM_THRESHOLD_ERROR_MSG),
    SAST_LOW_THRESHOLD_ERROR(12, SAST_LOW_THRESHOLD_ERROR_MSG),
    OSA_HIGH_THRESHOLD_ERROR(13, OSA_HIGH_THRESHOLD_ERROR_MSG),
    OSA_MEDIUM_THRESHOLD_ERROR(14, OSA_MEDIUM_THRESHOLD_ERROR_MSG),
    OSA_LOW_THRESHOLD_ERROR_EXIT_CODE(15, OSA_LOW_THRESHOLD_ERROR_MSG),

    POLICY_VIOLATION_ERROR(18, POLICY_VIOLATED_ERROR_MSG),
    GENERIC_THRESHOLD_FAILURE_ERROR(19, GENERIC_THRESHOLD_FAILURE_ERROR_MSG);

    private final int exitCode;
    private final String errorMessage;

    Errors(int exitCode, String errorMessage) {
        this.exitCode = exitCode;
        this.errorMessage = errorMessage;
    }

    public int getCode() {
        return exitCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
