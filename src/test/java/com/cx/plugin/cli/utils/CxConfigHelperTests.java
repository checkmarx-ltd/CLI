package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.DependencyScannerType;
import com.cx.restclient.sca.dto.SCAConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class CxConfigHelperTests {
    @Test
    public void ScaArgs_Valid() throws ParseException, CLIParsingException, URISyntaxException {
        final String LOCATION_PATH = "c:\\cxdev\\projectsToScan\\SastAndOsaSource",
                API_URL = "https://api.example.com",
                ACCESS_CONTROL_URL = "https://ac.example.com",
                WEB_APP_URL = "https://web.example.com",
                USERNAME = "myusername",
                PASSWORD = "mypassword",
                TENANT = "mytenant",
                FILE_INCLUDE = "includedfiles",
                FILE_EXCLUDE = "excludedfiles",
                PATH_EXCLUDE = "excludedpath";

        final Integer HIGH = 1,
                MEDIUM = 2,
                LOW = 3;

        final Command COMMAND = Command.SCA_SCAN;

        String[] args = new String[]{
                COMMAND.value(),
                "-projectname", "CxServer\\SP\\myprojectname",
                "-scalocationpath", LOCATION_PATH,
                "-scaapiurl", API_URL,
                "-scaaccesscontrolurl", ACCESS_CONTROL_URL,
                "-scawebappurl", WEB_APP_URL,
                "-scausername", USERNAME,
                "-scapassword", PASSWORD,
                "-scatenant", TENANT,
                "-scahigh", String.valueOf(HIGH),
                "-scamedium", String.valueOf(MEDIUM),
                "-scalow", String.valueOf(LOW),
                "-scafilesinclude", FILE_INCLUDE,
                "-scafilesexclude", FILE_EXCLUDE,
                "-scapathexclude", PATH_EXCLUDE
        };

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(Command.getOptions(), args);

        CxConfigHelper configHelper = new CxConfigHelper();
        CxScanConfig config = configHelper.resolveConfiguration(COMMAND, commandLine);
        assertNotNull(config);
        assertFalse(config.getSastEnabled());
        assertEquals(DependencyScannerType.SCA, config.getDependencyScannerType());
        assertTrue(StringUtils.isNotEmpty(config.getProjectName()));
        assertTrue(StringUtils.isNotEmpty(config.getTeamPath()));
        assertEquals(LOCATION_PATH, config.getOsaLocationPath());

        assertEquals(HIGH, config.getOsaHighThreshold());
        assertEquals(MEDIUM, config.getOsaMediumThreshold());
        assertEquals(LOW, config.getOsaLowThreshold());

        assertTrue(StringUtils.contains(config.getOsaFilterPattern(), FILE_INCLUDE));
        assertTrue(StringUtils.contains(config.getOsaFilterPattern(), FILE_EXCLUDE));
        assertEquals(PATH_EXCLUDE, config.getOsaFolderExclusions());

        SCAConfig sca = config.getScaConfig();
        assertNotNull(sca);
        assertEquals(ACCESS_CONTROL_URL, sca.getAccessControlUrl());
        assertEquals(API_URL, sca.getApiUrl());
        assertEquals(WEB_APP_URL, sca.getWebAppUrl());
        assertEquals(USERNAME, sca.getUsername());
        assertEquals(PASSWORD, sca.getPassword());
        assertEquals(TENANT, sca.getTenant());
    }
}