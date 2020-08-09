package com.cx.plugin.cli;

import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.utils.LegacyClient;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;

public class CxSastConnectionProvider extends LegacyClient {
    public CxSastConnectionProvider(CxScanConfig config, Logger log) throws MalformedURLException {
        super(config, log);
    }

    public boolean testConnection() {
        try {
            super.login();
            return true;
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }
}
