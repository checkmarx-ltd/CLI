package com.cx.plugin.cli;

import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.sast.utils.LegacyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

public class CxSastConnectionProvider extends LegacyClient {

    public static final Logger log = LoggerFactory.getLogger(CxSastConnectionProvider.class);

    public CxSastConnectionProvider(CxScanConfig config) throws MalformedURLException {
        super(config);
    }

}
