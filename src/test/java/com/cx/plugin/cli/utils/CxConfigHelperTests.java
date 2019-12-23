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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class CxConfigHelperTests {
    @Test
    @DisplayName("SCA-only scan: command line should be correctly converted to scan config")
    void ScaArgs_Valid() throws ParseException, CLIParsingException, URISyntaxException {
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

        String[] args = {
                Command.SCA_SCAN.value(),
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

        CommandLine commandLine = getCommandLine(args);

        CxConfigHelper configHelper = new CxConfigHelper();
        CxScanConfig config = configHelper.resolveConfiguration(Command.SCA_SCAN, commandLine);
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

    @Test
    @DisplayName("Exception should be thrown if we pass ambiguous arguments")
    void ScaArgs_Invalid() throws ParseException {
        assertException(Command.OSA_SCAN, new String[]{"-enableosa"});
        assertException(Command.SCA_SCAN, new String[]{"-enablesca"});
        assertException(Command.SCAN, new String[]{"-enablesca", "-enableosa"});
    }

    private void assertException(Command command, String[] args) throws ParseException {
        String[] fullArgs = ArrayUtils.add(args, 0, command.value());
        CommandLine commandLine = getCommandLine(fullArgs);
        CxConfigHelper configHelper = new CxConfigHelper();

        assertThrows(CLIParsingException.class, () ->
                configHelper.resolveConfiguration(command, commandLine));
    }

    private CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(Command.getOptions(), args);
    }
}