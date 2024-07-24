package com.cx.plugin.cli.utils;

import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.exceptions.BadOptionCombinationException;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.ast.dto.sca.AstScaConfig;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ScannerType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.naming.ConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CxConfigHelperTests {
    @Test
    @DisplayName("Command line should be correctly converted to scan config during a SCA-only scan")
    void ScaArgs_Valid() throws ParseException, CLIParsingException, IOException, ConfigurationException {
        final String LOCATION_PATH = "c:\\anydir\\child-dir",
                API_URL = "https://api.example.com",
                ACCESS_CONTROL_URL = "https://ac.example.com",
                WEB_APP_URL = "https://web.example.com",
                USERNAME = "myusername",
                PASSWORD = "mypassword",
                ACCOUNT = "myaccount",
                FILE_INCLUDE = "includedfiles",
                FILE_EXCLUDE = "excludedfiles",
                PATH_EXCLUDE = "excludedpath";

        final Integer CRITICAL = 1,
        		HIGH = 2,
                MEDIUM = 3,
                LOW = 4;

        final String[] DEFAULT_ARGS = {
                "-projectname", "CxServer\\SP\\myprojectname",
                "-scalocationpath", LOCATION_PATH,
                "-scaapiurl", API_URL,
                "-scaaccesscontrolurl", ACCESS_CONTROL_URL,
                "-scawebappurl", WEB_APP_URL,
                "-scausername", USERNAME,
                "-scapassword", PASSWORD,
                "-scaaccount", ACCOUNT,
                "-scacritical", String.valueOf(CRITICAL),
                "-scahigh", String.valueOf(HIGH),
                "-scamedium", String.valueOf(MEDIUM),
                "-scalow", String.valueOf(LOW),
                "-scafilesinclude", FILE_INCLUDE,
                "-scafilesexclude", FILE_EXCLUDE,
                "-scapathexclude", PATH_EXCLUDE
        };
        CommandLine commandLine = parseCommandLine(Command.SCA_SCAN, DEFAULT_ARGS);

        CxConfigHelper configHelper = new CxConfigHelper(null);
        CxScanConfig config = configHelper.resolveConfiguration(Command.SCA_SCAN, commandLine);
        assertNotNull(config);
        assertFalse(config.isSastEnabled());
        assertTrue(config.getScannerTypes().contains(ScannerType.AST_SCA));
        assertTrue(StringUtils.isNotEmpty(config.getProjectName()));
        assertTrue(StringUtils.isNotEmpty(config.getTeamPath()));
        assertEquals(LOCATION_PATH, config.getOsaLocationPath());

        assertEquals(HIGH, config.getOsaHighThreshold());
        assertEquals(MEDIUM, config.getOsaMediumThreshold());
        assertEquals(LOW, config.getOsaLowThreshold());

        assertTrue(StringUtils.contains(config.getOsaFilterPattern(), FILE_INCLUDE));
        assertTrue(StringUtils.contains(config.getOsaFilterPattern(), FILE_EXCLUDE));
        assertEquals(PATH_EXCLUDE, config.getOsaFolderExclusions());

        AstScaConfig sca = config.getAstScaConfig();
        assertNotNull(sca);
        assertEquals(ACCESS_CONTROL_URL, sca.getAccessControlUrl());
        assertEquals(API_URL, sca.getApiUrl());
        assertEquals(WEB_APP_URL, sca.getWebAppUrl());
        assertEquals(USERNAME, sca.getUsername());
        assertEquals(PASSWORD, sca.getPassword());
        assertEquals(ACCOUNT, sca.getTenant());
    }

    @Test
    @DisplayName("Exception should be thrown if we pass invalid combinations of arguments")
    void ScaArgs_InvalidArgCombination() throws ParseException {
        String[] invalidOsaArgs = {
                "-enablesca", "-cxserver", "myhostname", "-cxuser", "user", "-cxpassword", "pwd",
                "-locationtype", "folder", "-locationpath", "c:\\anydir\\child-dir"};
        assertException(Command.OSA_SCAN, invalidOsaArgs);

        String[] invalidScaArgs = {"-enableosa"};
        assertException(Command.SCA_SCAN, invalidScaArgs);

        String[] invalidSastArgs = {"-enablesca", "-enableosa"};
        assertException(Command.SCAN, invalidSastArgs);
    }

    private void assertException(Command command, String[] args) throws ParseException {
        CommandLine commandLine = parseCommandLine(command, args);
        CxConfigHelper configHelper = new CxConfigHelper(null);

        BadOptionCombinationException ex = assertThrows(BadOptionCombinationException.class, () ->
                configHelper.resolveConfiguration(command, commandLine));
    }

    private CommandLine parseCommandLine(Command command, String[] args) throws ParseException {
        String[] fullArgs = ArrayUtils.add(args, 0, command.value());
        CommandLineParser parser = new DefaultParser();
        return parser.parse(Command.getOptions(), fullArgs);
    }
}