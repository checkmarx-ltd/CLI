package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.restclient.dto.scansummary.ErrorSource;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.dto.scansummary.ThresholdError;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.cx.plugin.cli.errorsconstants.ErrorMessages.*;

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
        messageToCodeMap.put(OSA_LOW_THRESHOLD_ERROR_MSG, Errors.OSA_LOW_THRESHOLD_ERROR.getCode());
        // SAST thresholds
        messageToCodeMap.put(SAST_HIGH_THRESHOLD_ERROR_MSG, Errors.SAST_HIGH_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(SAST_MEDIUM_THRESHOLD_ERROR_MSG, Errors.SAST_MEDIUM_THRESHOLD_ERROR.getCode());
        messageToCodeMap.put(SAST_LOW_THRESHOLD_ERROR_MSG, Errors.SAST_LOW_THRESHOLD_ERROR.getCode());

        return messageToCodeMap;
    }

    public static Errors getErrorType(ScanSummary scanSummary) {
        if (scanSummary.isPolicyViolated()) {
            return Errors.POLICY_VIOLATION_ERROR;
        }

        ThresholdError sastMostSevere = getMostSevereThresholdError(scanSummary, ErrorSource.SAST);
        ThresholdError dsMostSevere = getMostSevereThresholdError(scanSummary, ErrorSource.DEPENDENCY_SCANNER);
        if (sastMostSevere != null && dsMostSevere != null) {
            return Errors.GENERIC_THRESHOLD_FAILURE_ERROR;
        } else if (sastMostSevere == null && dsMostSevere == null) {
            return Errors.SCAN_SUCCEEDED;
        } else {
            return toThresholdErrorCode(sastMostSevere != null ? sastMostSevere : dsMostSevere);
        }
    }

    private static Errors toThresholdErrorCode(ThresholdError error) {
        if (error.getSource() == ErrorSource.SAST) {
            switch (error.getSeverity()) {
                case HIGH:
                    return Errors.SAST_HIGH_THRESHOLD_ERROR;
                case MEDIUM:
                    return Errors.SAST_MEDIUM_THRESHOLD_ERROR;
                default:
                    return Errors.SAST_LOW_THRESHOLD_ERROR;
            }
        } else {
            switch (error.getSeverity()) {
                case HIGH:
                    return Errors.OSA_HIGH_THRESHOLD_ERROR;
                case MEDIUM:
                    return Errors.OSA_MEDIUM_THRESHOLD_ERROR;
                default:
                    return Errors.OSA_LOW_THRESHOLD_ERROR;
            }
        }
    }

    private static ThresholdError getMostSevereThresholdError(ScanSummary scanSummary, ErrorSource source) {
        return scanSummary.getThresholdErrors()
                .stream()
                .filter(error -> error.getSource() == source)
                .max(Comparator.comparing(ThresholdError::getSeverity))
                .orElse(null);
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
