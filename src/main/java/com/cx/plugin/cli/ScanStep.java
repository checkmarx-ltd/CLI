package com.cx.plugin.cli;

import com.cx.plugin.cli.errorsconstants.Errors;
import com.cx.plugin.cli.utils.ErrorParsingHelper;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Results;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.scansummary.ScanSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ScanStep implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ScanStep.class);

    CxScanConfig cxScanConfig;
    CxClientDelegator clientDelegator;
    List<ScanResults> results;

    public ScanStep(CxScanConfig cxScanConfig,
                    CxClientDelegator clientDelegator,
                    List<ScanResults> results) {
        this.cxScanConfig = cxScanConfig;
        this.clientDelegator = clientDelegator;
        this.results = results;
    }

    @Override
    public Integer call() throws Exception {
        int exitCode = Errors.SCAN_SUCCEEDED.getCode();
        ScanResults createScanResults = clientDelegator.initiateScan();
        results.add(createScanResults);

        if (cxScanConfig.getSynchronous()) {
            final ScanResults scanResults = clientDelegator.waitForScanResults();
            results.add(scanResults);

            getScanResultExceptionIfExists(results);

            ScanSummary scanSummary = new ScanSummary(
                    cxScanConfig,
                    scanResults.getSastResults(),
                    scanResults.getOsaResults(),
                    scanResults.getScaResults()
            );
            String scanSummaryString = scanSummary.toString();
            if (scanSummary.hasErrors()) {
                log.info(scanSummaryString);
                exitCode = ErrorParsingHelper.getErrorType(scanSummary).getCode();
            }
        } else {
            getScanResultExceptionIfExists(results);
        }
        return exitCode;
    }

    private static void getScanResultExceptionIfExists(List<ScanResults> scanResults) {
        scanResults.forEach(scanResult -> {
            if (scanResult != null) {
                Map<ScannerType, Results> resultsMap = scanResult.getResults();
                for (Results value : resultsMap.values()) {
                    if (value != null && value.getException() != null) {
                        throw value.getException();
                    }
                }
            }
        });
    }

}