package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.errorsconstants.Errors;

public class ErrorParsingHelper {

    public static int resolveThresholdFailures(String failureResult) {
        int errorCode = 0;

        if (failureResult.contains("CxSAST high")) {
            errorCode = Errors.SAST_HIGH_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST medium")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.SAST_MEDIUM_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST low")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.SAST_LOW_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA high")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_HIGH_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA medium")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_MEDIUM_THRESHOLD_ERROR.getCode();
        }

        if (failureResult.contains("CxOSA low")) {
            if (errorCode != 0) {
                return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
            }
            errorCode = Errors.OSA_LOW_THRESHOLD_ERROR_EXIT_CODE.getCode();
        }

        return errorCode;
    }

    //TODO: should map exception messages to corresponding code error
    public static int parseError(Exception e) {
        return 1;
    }
}
