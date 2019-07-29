package com.cx.plugin.cli.errorsconstants;

/**
 * Created by idanA on 11/5/2018.
 */
public enum ExitCodes {
    SCAN_SUCCEEDED_EXIT_CODE(0),
    GENERAL_ERROR_EXIT_CODE(1),
    SDLC_ERROR_EXIT_CODE(2),
    NO_OSA_LICENSE_ERROR_EXIT_CODE(3),
    LOGIN_FAILED_ERROR_EXIT_CODE(4),
    NO_PROJECT_PRIOR_TO_OSA_SCAN_ERROR_EXIT_CODE(5),

    SAST_HIGH_THRESHOLD_ERROR_EXIT_CODE(10),
    SAST_MEDIUM_THRESHOLD_ERROR_EXIT_CODE(11),
    SAST_LOW_THRESHOLD_ERROR_EXIT_CODE(12),
    OSA_HIGH_THRESHOLD_ERROR_EXIT_CODE(13),
    OSA_MEDIUM_THRESHOLD_ERROR_EXIT_CODE(14),
    OSA_LOW_THRESHOLD_ERROR_EXIT_CODE(15),

    POLICY_VIOLATION_ERROR_EXIT_CODE(18),
    GENERIC_THRESHOLD_FAILURE_ERROR_EXIT_CODE(19),

    CANCELED_BY_USER(130);

    private final int exitCode;

    ExitCodes(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getValue() {
        return exitCode;
    }
}
