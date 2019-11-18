package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.errorsconstants.Errors;
import static com.cx.plugin.cli.errorsconstants.ErrorMessages.*;

import java.util.HashMap;
import java.util.Map;

public class ErrorParsingHelper {

    private ErrorParsingHelper() {
        throw new IllegalStateException("Utility class");
    }

    private static Map<String, Integer> errorMsgToCodeMap = createMessageToCodeMap();

    private static Map<String, Integer> createMessageToCodeMap() {
        Map<String, Integer> messageToCodeMap = new HashMap<>();

        messageToCodeMap.put(LOGIN_FAILED_MSG, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(UNSUCCESSFUL_LOGIN_ERROR_MSG, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(UNSUCCESSFUL_REST_LOGIN, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(INVALID_CREDENTIALS_FOR_TOKEN_GENERATION, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(INVALID_ACCESS_TOKEN, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(INVALID_CREDENTIALS, Errors.LOGIN_FAILED_ERROR.getCode());
        messageToCodeMap.put(SDLC_ERROR_MSG, Errors.SDLC_ERROR.getCode());
        messageToCodeMap.put(NO_OSA_LICENSE_ERROR_MSG, Errors.NO_OSA_LICENSE_ERROR.getCode());
        messageToCodeMap.put(NO_PROJECT_PRIOR_TO_OSA_SCAN_ERROR_MSG, Errors.NO_PROJECT_PRIOR_TO_OSA_SCAN_ERROR.getCode());
        messageToCodeMap.put(REPORT_PARAMETER_IN_ASYNC_SCAN, Errors.GENERAL_ERROR.getCode());
        messageToCodeMap.put(THRESHOLD_PARAMETER_IN_ASYNC_SCAN, Errors.GENERAL_ERROR.getCode());
        // Generic threshold
        messageToCodeMap.put(GENERIC_THRESHOLD_FAILURE_ERROR_MSG, Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode());
        // OSA thresholds
        messageToCodeMap.put(OSA_HIGH_THRESHOLD_ERROR_MSG, Errors.OSA_HIGH_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(OSA_MEDIUM_THRESHOLD_ERROR_MSG, Errors.OSA_MEDIUM_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(OSA_LOW_THRESHOLD_ERROR_MSG, Errors.OSA_LOW_THRESHOLD_ERROR_EXIT_CODE.getCode());
        // SAST thresholds
        messageToCodeMap.put(SAST_HIGH_THRESHOLD_ERROR_MSG, Errors.SAST_HIGH_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(SAST_MEDIUM_THRESHOLD_ERROR_MSG, Errors.SAST_MEDIUM_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(SAST_LOW_THRESHOLD_ERROR_MSG, Errors.SAST_LOW_THRESHOLD_ERROR.getCode());

        return messageToCodeMap;
    }

    private static int resolveSastThresholdFailure(String failureResult) {
        if (failureResult.contains("CxSAST high")) {
            return Errors.SAST_HIGH_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST medium")) {
            return Errors.SAST_MEDIUM_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxSAST low")) {
            return Errors.SAST_LOW_THRESHOLD_ERROR.getCode();
        }
        return 0;
    }

    private static int resolveOsaThresholdFailure(String failureResult) {
        if (failureResult.contains("CxOSA high")) {
            return Errors.OSA_HIGH_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxOSA medium")) {
            return Errors.OSA_MEDIUM_THRESHOLD_ERROR.getCode();
        }
        if (failureResult.contains("CxOSA low")) {
            return Errors.OSA_LOW_THRESHOLD_ERROR_EXIT_CODE.getCode();
        }
        return 0;
    }

    public static int resolveThresholdFailures(String failureResult) {
        if(failureResult.contains("Project policy status : violated")) {
            return Errors.POLICY_VIOLATION_ERROR.getCode();
        }

        int sastErrorCode = resolveSastThresholdFailure(failureResult);
        int osaErrorCode = resolveOsaThresholdFailure(failureResult);

        if(sastErrorCode != 0 && osaErrorCode != 0) {
            return Errors.GENERIC_THRESHOLD_FAILURE_ERROR.getCode();
        }
        else if (sastErrorCode != 0 && osaErrorCode == 0) {
            return sastErrorCode;
        }
        else if (sastErrorCode == 0 && osaErrorCode != 0) {
            return osaErrorCode;
        }
        return 0;
    }

    public static int parseError(String errorMsg) {
        if (errorMsg == null) {
            return Errors.GENERAL_ERROR.getCode();
        }

        for (Map.Entry<String, Integer> entry : errorMsgToCodeMap.entrySet()) {
            if (errorMsg.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return Errors.GENERAL_ERROR.getCode();
    }
}
