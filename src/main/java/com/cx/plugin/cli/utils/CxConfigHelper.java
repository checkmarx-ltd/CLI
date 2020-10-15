package com.cx.plugin.cli.utils;

import com.checkmarx.configprovider.ConfigProvider;
import com.checkmarx.configprovider.dto.GeneralRepoDto;
import com.checkmarx.configprovider.dto.ProtocolType;
import com.checkmarx.configprovider.dto.ResourceType;
import com.checkmarx.configprovider.dto.interfaces.ConfigReader;
import com.checkmarx.configprovider.readers.FileReader;
import com.checkmarx.configprovider.readers.GeneralGitReader;
import com.cx.plugin.cli.configascode.ConfigAsCode;
import com.cx.plugin.cli.configascode.ProjectConfig;
import com.cx.plugin.cli.configascode.SastConfig;
import com.cx.plugin.cli.configascode.ScaConfig;
import com.cx.plugin.cli.constants.Command;
import com.cx.plugin.cli.constants.Parameters;
import com.cx.plugin.cli.exceptions.BadOptionCombinationException;
import com.cx.plugin.cli.exceptions.CLIParsingException;
import com.cx.restclient.ast.dto.sca.AstScaConfig;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.RemoteSourceTypes;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.SourceLocationType;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.transport.URIish;

import javax.annotation.Nullable;
import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cx.plugin.cli.constants.Parameters.*;
import static com.cx.plugin.cli.utils.PropertiesManager.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Created by idanA on 11/5/2018.
 */
public final class CxConfigHelper {

    private static final String CONFIG_AS_CODE_FILE_NAME = "cx.config";
    public static final String CONFIG_FILE_S_NOT_FOUND_OR_COULDN_T_BE_LOADED = "Config file %s not found or couldn't " +
            "be loaded,please check if file exist's and if file content is valid";
    private static Logger log = LogManager.getLogger(CxConfigHelper.class);

    private static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static PropertiesManager props;

    private Command command;
    private CommandLine commandLine;

    public CxConfigHelper(String configFilePath) {
        props = PropertiesManager.getProps(configFilePath);
    }

    /**
     * Resolves configuration from the config file and the console parameters
     *
     * @param command the first command line argument mapped to an enum.
     * @param cmd     the parameters passed by the user mapped by key/value
     * @return CxScanConfig an object containing all the relevant data for the scan
     */
    public CxScanConfig resolveConfiguration(Command command, CommandLine cmd) throws CLIParsingException, IOException, ConfigurationException {
        this.command = command;
        this.commandLine = cmd;

        checkForAmbiguousArgs();

        CxScanConfig scanConfig = new CxScanConfig();

        if (testConnection(scanConfig)) {
            return scanConfig;
        }

        scanConfig.setProxyConfig(genProxyConfig());
        scanConfig.setNTLM(cmd.hasOption(NTLM));

        if (command.equals(Command.SCAN) || command.equals(Command.ASYNC_SCAN)) {
            scanConfig.setSastEnabled(true);
            scanConfig.addScannerType(ScannerType.SAST);

        }

        scanConfig.setSynchronous(command.equals(Command.SCAN) ||
                command.equals(Command.OSA_SCAN) ||
                command.equals(Command.SCA_SCAN));


        ScannerType scannerType = getDependencyScannerType();

        if (scannerType != null)
            scanConfig.addScannerType(scannerType);


        scanConfig.setCxOrigin(CX_ORIGIN);

        if (scanConfig.isSastOrOSAEnabled() || command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            scanConfig.setUrl(getSastOrOsaServerUrl());
            setSastOrOsaCredentials(scanConfig);
        }

        if (command.equals(Command.GENERATE_TOKEN) || command.equals(Command.REVOKE_TOKEN)) {
            return scanConfig;
        }

        scanConfig.setEngineConfigurationName(cmd.getOptionValue(CONFIGURATION));

        scanConfig.setUseSSOLogin(cmd.hasOption(IS_SSO));
        scanConfig.setDisableCertificateValidation(cmd.hasOption(TRUSTED_CERTIFICATES));

        scanConfig.setPublic(!cmd.hasOption(IS_PRIVATE));
        scanConfig.setEnablePolicyViolations(cmd.hasOption(IS_CHECKED_POLICY));
        if (!commandLine.hasOption(CONFIG_AS_CODE)) {
            if ((command.equals(Command.SCA_SCAN)) || (command.equals(Command.ASYNC_SCA_SCAN))) {
                scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH), true));
            } else {
                scanConfig.setProjectName(extractProjectName(cmd.getOptionValue(FULL_PROJECT_PATH), false));
                scanConfig.setTeamPath(extractTeamPath(cmd.getOptionValue(FULL_PROJECT_PATH)));
            }
        }

        scanConfig.setPresetName(cmd.getOptionValue(PRESET) == null ? DEFAULT_PRESET_NAME : cmd.getOptionValue(PRESET));

        scanConfig.setSastFolderExclusions(getParamWithDefault(LOCATION_PATH_EXCLUDE, KEY_EXCLUDED_FOLDERS, true));
        String includeExcludeCommand = getRelevantCommand();
        String sastFilterPattern = getParamWithDefault(includeExcludeCommand, KEY_EXCLUDED_FILES, true) + getIncExcParamWithDefault(includeExcludeCommand);
        scanConfig.setSastFilterPattern(sastFilterPattern);
        scanConfig.setScanComment(cmd.getOptionValue(SCAN_COMMENT));
        setScanReports(scanConfig);
        scanConfig.setIncremental(cmd.hasOption(IS_INCREMENTAL));
        scanConfig.setForceScan(cmd.hasOption(IS_FORCE_SCAN));
        setSASTThresholds(scanConfig);

        String dsLocationPath = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        if (scanConfig.isSastEnabled() || dsLocationPath == null) {
            ScanSourceConfigurator locator = new ScanSourceConfigurator();
            locator.configureSourceLocation(commandLine, props, scanConfig);
        }

        scanConfig.setProgressInterval(props.getIntProperty(KEY_PROGRESS_INTERVAL));
        scanConfig.setConnectionRetries(props.getIntProperty(KEY_RETRIES));
        scanConfig.setDefaultProjectName(props.getProperty(KEY_DEF_PROJECT_NAME));

        configureDependencyScan(scanConfig);

        if (cmd.hasOption(CONFIG_AS_CODE))
            checkForConfigAsCode(scanConfig);

        return scanConfig;
    }

    private void checkForConfigAsCode(CxScanConfig scanConfig) throws CLIParsingException, IOException, ConfigurationException {
        if (StringUtils.isNotEmpty(scanConfig.getRemoteSrcUrl()) && RemoteSourceTypes.GIT == scanConfig.getRemoteType()) {
            resolveConfigAsCodeFromRemote(scanConfig);
        }

        if (StringUtils.isNotEmpty(scanConfig.getSourceDir())) {
            resolveConfigAsCodeLocal(scanConfig);
        }
    }

    private void resolveConfigAsCodeLocal(CxScanConfig scanConfig) throws CLIParsingException, IOException, ConfigurationException {
        log.info("loading config file from local working directory ..");

            ConfigAsCode configAsCodeFromFile = getConfigAsCodeFromFile(
                    scanConfig.getSourceDir() + File.separator + ".checkmarx" + File.separator + CONFIG_AS_CODE_FILE_NAME);
            overrideConfigAsCode(configAsCodeFromFile, scanConfig);

    }

    private void resolveConfigAsCodeFromRemote(CxScanConfig scanConfig) throws CLIParsingException, ConfigurationException {
        log.info("loading config file from remote working directory ..");
        GeneralGitReader reader = null;


        try {
            URIish urIish = prepareRemoteUrl(scanConfig.getRemoteSrcUrl(), scanConfig.getRemoteSrcPort());
            GeneralRepoDto repoDto = new GeneralRepoDto();
            repoDto.setSrcUrl(urIish.toString());
            repoDto.setSrcPass(isNotEmpty(urIish.getPass()) ? urIish.getPass() : scanConfig.getRemoteSrcPass());
            repoDto.setSrcUserName(isNotEmpty(urIish.getUser()) ? urIish.getUser() : scanConfig.getRemoteSrcUser());
            repoDto.setSrcRef(scanConfig.getRemoteSrcBranch());

            if ("http".equalsIgnoreCase(urIish.getScheme()) || "https".equalsIgnoreCase(urIish.getScheme())) {
                repoDto.setProtocolType(ProtocolType.HTTP);

            } else {
                repoDto.setProtocolType(ProtocolType.SSH);
                repoDto.setSrcPrivateKey(commandLine.getOptionValue(PRIVATE_KEY));
            }

            reader = new GeneralGitReader(repoDto);

            ConfigAsCode configAsCodeFromFile = getConfigAsCode(reader);

            overrideConfigAsCode(configAsCodeFromFile, scanConfig);

        } catch (URISyntaxException e) {
            throw new ConfigurationException(String.format("Couldn't initiate connection using %s", scanConfig.getRemoteSrcUrl()));
        } finally {
            Optional.ofNullable(reader).ifPresent(GeneralGitReader::close);
        }
    }

    private ConfigAsCode getConfigAsCode(ConfigReader reader) throws ConfigurationException {
        ConfigProvider configProvider = ConfigProvider.getInstance();

        configProvider.init(CX_ORIGIN, reader);

        if (!configProvider.hasAnyConfiguration(CX_ORIGIN))
            throw new ConfigurationException(String.format(CONFIG_FILE_S_NOT_FOUND_OR_COULDN_T_BE_LOADED, ".checkmarx/"+CONFIG_AS_CODE_FILE_NAME));


        ConfigAsCode configAsCodeFromFile = new ConfigAsCode();

        if (configProvider.hasConfiguration(CX_ORIGIN, "project"))
            configAsCodeFromFile.setProject(
                    configProvider.getConfiguration(CX_ORIGIN, "project", ProjectConfig.class));

        if (configProvider.hasConfiguration(CX_ORIGIN, "sast"))
            configAsCodeFromFile.setSast(
                    configProvider.getConfiguration(CX_ORIGIN, "sast", SastConfig.class));

        if (configProvider.hasConfiguration(CX_ORIGIN, "sca"))
            configAsCodeFromFile.setSca(
                    configProvider.getConfiguration(CX_ORIGIN, "sca", ScaConfig.class));
        return configAsCodeFromFile;
    }

    private void overrideConfigAsCode(ConfigAsCode configAsCodeFromFile, CxScanConfig scanConfig) throws CLIParsingException {
        Map<String, String> overridesResults = new HashMap<>();

        mapProjectConfiguration(Optional.ofNullable(configAsCodeFromFile.getProject()), scanConfig, overridesResults);
        mapSastConfiguration(Optional.ofNullable(configAsCodeFromFile.getSast()), scanConfig, overridesResults);
        mapScaConfiguration(Optional.ofNullable(configAsCodeFromFile.getSca()), scanConfig, overridesResults);

        if (!overridesResults.isEmpty()) {
            log.info("the following fields was overrides using config as code file : ");
            overridesResults.keySet().forEach(key -> log.info(String.format("%s = %s", key, overridesResults.get(key))));
        }
    }

    private void mapScaConfiguration(Optional<ScaConfig> sca, CxScanConfig scanConfig, Map<String, String> overridesResults) {

        AtomicReference<String> fileInclude = new AtomicReference<>("");
        AtomicReference<String> fileExclude = new AtomicReference<>("");

        sca.map(ScaConfig::getFileExclude)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    fileExclude.set(pValue);
                    overridesResults.put("Sca File Exclude", pValue);
                });

        sca.map(ScaConfig::getFileInclude)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    fileInclude.set(pValue);
                    overridesResults.put("Sca File Include", pValue);
                });

        sca.map(ScaConfig::getPathExclude)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    scanConfig.setOsaFolderExclusions(pValue);
                    overridesResults.put("Sca Folder Exclude", pValue);
                });

        sca.map(ScaConfig::getHigh)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setOsaThresholdsEnabled(true);
                    scanConfig.setOsaHighThreshold(pValue);
                    overridesResults.put("Sca High", String.valueOf(pValue));
                });

        sca.map(ScaConfig::getMedium)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setOsaThresholdsEnabled(true);
                    scanConfig.setOsaMediumThreshold(pValue);
                    overridesResults.put("Sca Medium", String.valueOf(pValue));
                });

        sca.map(ScaConfig::getLow)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setOsaThresholdsEnabled(true);
                    scanConfig.setOsaLowThreshold(pValue);
                    overridesResults.put("Sca Low", String.valueOf(pValue));
                });

        //build include/exclude file pattern
        if (!fileExclude.get().isEmpty() || !fileInclude.get().isEmpty())
            setDependencyScanFilterPattern(scanConfig, fileInclude.get(), fileExclude.get());

    }

    private void mapSastConfiguration(Optional<SastConfig> sast, CxScanConfig scanConfig, Map<String, String> overridesResults) {
        sast.map(SastConfig::getConfiguration)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    scanConfig.setEngineConfigurationName(pValue);
                    overridesResults.put("Configuration", pValue);
                });

        sast.map(SastConfig::isIncremental)
                .ifPresent(pValue -> {
                    scanConfig.setIncremental(pValue);
                    overridesResults.put("Is Incremental", String.valueOf(pValue));
                });

        sast.map(SastConfig::isPrivateScan)
                .ifPresent(pValue -> {
                    scanConfig.setPublic(!pValue);
                    overridesResults.put("Is Private", String.valueOf(pValue));
                });

        sast.map(SastConfig::getLow)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setSastThresholdsEnabled(true);
                    scanConfig.setSastLowThreshold(pValue);
                    overridesResults.put("Low", String.valueOf(pValue));
                });

        sast.map(SastConfig::getMedium)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setSastThresholdsEnabled(true);
                    scanConfig.setSastMediumThreshold(pValue);
                    overridesResults.put("Medium", String.valueOf(pValue));
                });

        sast.map(SastConfig::getHigh)
                .filter(n -> n > 0)
                .ifPresent(pValue -> {
                    scanConfig.setSastThresholdsEnabled(true);
                    scanConfig.setSastHighThreshold(pValue);
                    overridesResults.put("High", String.valueOf(pValue));
                });
        sast.map(SastConfig::getPreset)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    scanConfig.setPresetName(pValue);
                    overridesResults.put("Preset", pValue);
                });

        sast.map(SastConfig::getExcludeFolders)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    if (StringUtils.isNotEmpty(scanConfig.getSastFolderExclusions())) {
                        pValue = Stream.of(scanConfig.getSastFolderExclusions().split(","), pValue.split(","))
                                .flatMap(x -> Arrays.stream(x))
                                .map(String::trim)
                                .distinct()
                                .collect(Collectors.joining(","));
                    }
                    scanConfig.setSastFolderExclusions(pValue);
                    overridesResults.put("Folder Exclusions", pValue);
                });

        sast.map(SastConfig::getIncludeExcludePattern)
                .filter(StringUtils::isNotBlank)
                .ifPresent(pValue -> {
                    if (StringUtils.isNotEmpty(scanConfig.getSastFilterPattern())) {
                        pValue = Stream.of(scanConfig.getSastFilterPattern().split(","), pValue.split(","))
                                .flatMap(x -> Arrays.stream(x))
                                .map(String::trim)
                                .distinct()
                                .collect(Collectors.joining(","));
                    }
                    scanConfig.setSastFilterPattern(pValue);
                    overridesResults.put("Include/Exclude pattern", pValue);
                });
    }

    private void mapProjectConfiguration(Optional<ProjectConfig> project, CxScanConfig scanConfig, Map<String, String> overridesResults) throws CLIParsingException {

        String projectName = project.map(ProjectConfig::getFullPath).orElse(null);

        if ((command.equals(Command.SCA_SCAN)) || (command.equals(Command.ASYNC_SCA_SCAN))) {
            scanConfig.setProjectName(extractProjectName(projectName, true));
        } else {
            scanConfig.setProjectName(extractProjectName(projectName, false));
            scanConfig.setTeamPath(extractTeamPath(projectName));
        }


        if (projectName != null)
            overridesResults.put("Project Name", projectName);


        project.map(ProjectConfig::getOrigin)
                .filter(StringUtils::isNotBlank)
                .ifPresent(origin -> {
                    scanConfig.setCxOrigin(origin);
                    overridesResults.put("Origin", origin);
                });
    }

    private ConfigAsCode getConfigAsCodeFromFile(String filePath) throws ConfigurationException, IOException {

        com.checkmarx.configprovider.readers.FileReader reader = new FileReader(ResourceType.YAML, filePath);

        return getConfigAsCode(reader);


    }


    private URIish prepareRemoteUrl(String remoteSrcUrl, int remoteSrcPort) throws URISyntaxException {
        URIish urIish = new URIish(remoteSrcUrl);
        if (remoteSrcPort > 0) {
            urIish.setPort(remoteSrcPort);
        }

        return urIish;
    }

    private void configureDependencyScan(CxScanConfig scanConfig) throws CLIParsingException {
        if (!scanConfig.isAstScaEnabled() && !scanConfig.isOsaEnabled()) {
            return;
        }

        if (scanConfig.isOsaEnabled()) {
            setOsaSpecificConfig(scanConfig);
        } else if (scanConfig.isAstScaEnabled()) {
            setScaSpecificConfig(scanConfig);
        }

        setSharedDependencyScanConfig(scanConfig);
    }

    private String getRelevantCommand() {
        String oldCommandLineValue = commandLine.getOptionValue(LOCATION_FILES_EXCLUDE);
        String newCommandLineValue = commandLine.getOptionValue(INCLUDE_EXCLUDE_PATTERN);
        if (newCommandLineValue != null) {
            return INCLUDE_EXCLUDE_PATTERN;
        } else if (oldCommandLineValue != null) {
            return LOCATION_FILES_EXCLUDE;
        }
        return "";
    }

    private void setOsaSpecificConfig(CxScanConfig scanConfig) {
        scanConfig.setOsaRunInstall(commandLine.hasOption(INSTALL_PACKAGE_MANAGER));

        String reportDir = commandLine.getOptionValue(OSA_JSON_REPORT);
        scanConfig.setReportsDir(reportDir != null ? new File(reportDir) : null);
        scanConfig.setOsaGenerateJsonReport(reportDir != null);

        String archiveIncludePatterns = getParamWithDefault(OSA_ARCHIVE_TO_EXTRACT, KEY_OSA_EXTRACTABLE_INCLUDE_FILES, false);
        scanConfig.setOsaArchiveIncludePatterns(archiveIncludePatterns);

        String osaScanDepth = getOptionalParam(OSA_SCAN_DEPTH, KEY_OSA_SCAN_DEPTH);
        scanConfig.setOsaScanDepth(osaScanDepth);
    }


    private void setScaSpecificConfig(CxScanConfig scanConfig) throws CLIParsingException {
        AstScaConfig sca = new AstScaConfig();

        String apiUrl = normalizeUrl(getRequiredParam(commandLine, SCA_API_URL, KEY_SCA_API_URL));
        sca.setApiUrl(apiUrl);

        String acUrl = normalizeUrl(getRequiredParam(commandLine, SCA_ACCESS_CONTROL_URL, KEY_SCA_ACCESS_CONTROL_URL));
        sca.setAccessControlUrl(acUrl);

        String webAppUrl = normalizeUrl(getRequiredParam(commandLine, SCA_WEB_APP_URL, KEY_SCA_WEB_APP_URL));
        sca.setWebAppUrl(webAppUrl);

        String envVariables = getOptionalParam(ENV_VARIABLE, "");
        if(StringUtils.isNotEmpty(envVariables))
        {
            sca.setEnvVariables(convertStringToKeyValueMap(envVariables));
        }

        sca.setSastProjectId(getOptionalParam(SAST_PROJECT_ID, ""));
        sca.setSastServerUrl(getOptionalParam(SERVER_URL, ""));
        sca.setSastUsername(getOptionalParam(USER_NAME, ""));
        sca.setSastPassword(getOptionalParam(USER_PASSWORD, ""));

        sca.setUsername(getRequiredParam(commandLine, SCA_USERNAME, null));
        sca.setPassword(getRequiredParam(commandLine, SCA_PASSWORD, null));
        sca.setTenant(getRequiredParam(commandLine, SCA_ACCOUNT, null));

        sca.setRemoteRepositoryInfo(null);
        sca.setSourceLocationType(SourceLocationType.LOCAL_DIRECTORY);

        String reportDir = commandLine.getOptionValue(SCA_JSON_REPORT);
        if (reportDir != null) {
            File reportFolder = new File(reportDir);
            Path reportPath = Paths.get(reportDir);
            if (reportFolder.isDirectory() && reportFolder.canWrite()) {
                if (Files.isWritable(reportPath)) {
                    scanConfig.setReportsDir(reportDir != null ? reportFolder : null);
                    //use setOsaGenerateJsonReport instead of creating one for sca, because there is no case of using osa and sca simultaneously.
                    scanConfig.setOsaGenerateJsonReport(reportDir != null);
                    scanConfig.setScaJsonReport(reportDir);
                } else {
                    throw new CLIParsingException("There is no write access for: " + reportDir);
                }
            } else {
                throw new CLIParsingException(reportDir + " directory doesn't exist.");
            }
        }

        scanConfig.setAstScaConfig(sca);
    }

    private HashMap<String, String> convertStringToKeyValueMap(String envString) {

        HashMap<String, String> envMap = new HashMap<>();
        //"Key1=Val1,Key2=Val2"
        String trimmedString = envString.replace("\"","");
        List<String> envlist = Arrays.asList(trimmedString.split(","));

        for( String variable : envlist)
        {
            String[] splitFromEqual = variable.split("=");
            String key = (splitFromEqual[0]).trim();
            String value = (splitFromEqual[1]).trim();

            envMap.put(key, value);
        }
        return envMap;

    }

    private void setSharedDependencyScanConfig(CxScanConfig scanConfig) {
        setDependencyScanThresholds(scanConfig);

        String location = getSharedDependencyScanOption(scanConfig, OSA_LOCATION_PATH, SCA_LOCATION_PATH);
        scanConfig.setOsaLocationPath(location);

        String folderExclusions = getSharedDependencyScanOption(scanConfig, OSA_FOLDER_EXCLUDE, SCA_FOLDER_EXCLUDE);
        scanConfig.setOsaFolderExclusions(folderExclusions);

        String key = scanConfig.isOsaEnabled() ?
                KEY_OSA_PROGRESS_INTERVAL :
                KEY_SCA_PROGRESS_INTERVAL;
        scanConfig.setOsaProgressInterval(props.getIntProperty(key));

        setDependencyScanFilterPattern(scanConfig);
    }

    private void setDependencyScanFilterPattern(CxScanConfig scanConfig) {
        String includedFiles, excludedFiles;
        if (scanConfig.isOsaEnabled()) {
            includedFiles = getOptionalParam(OSA_FILES_INCLUDE, KEY_OSA_INCLUDED_FILES);
            excludedFiles = getOptionalParam(OSA_FILES_EXCLUDE, KEY_OSA_EXCLUDED_FILES);
        } else {
            includedFiles = getOptionalParam(SCA_FILES_INCLUDE, KEY_SCA_INCLUDED_FILES);
            excludedFiles = getOptionalParam(SCA_FILES_EXCLUDE, KEY_SCA_EXCLUDED_FILES);
        }


        setDependencyScanFilterPattern(scanConfig, includedFiles, excludedFiles);
    }

    private void setDependencyScanFilterPattern(CxScanConfig scanConfig, String includedFiles, String excludedFiles) {
        String filterPattern = null;
        if (includedFiles != null) {
            if (excludedFiles != null) {
                filterPattern = includedFiles + ", " + excludedFiles;
            } else
                filterPattern = includedFiles;
        } else if (excludedFiles != null) {
            filterPattern = excludedFiles;
        }

        scanConfig.setOsaFilterPattern(filterPattern);
    }

    private void setSastOrOsaCredentials(CxScanConfig scanConfig) throws CLIParsingException {
        boolean isSSO = commandLine.hasOption(IS_SSO);
        String token = commandLine.getOptionValue(TOKEN);
        String username = commandLine.getOptionValue(USER_NAME);
        String password = validatePassword(commandLine.getOptionValue(USER_PASSWORD));
        if (isNotEmpty(token)) {
            scanConfig.setRefreshToken(token);
        } else if ((Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) && !isSSO) {
            throw new CLIParsingException("[CxConsole] User name and password are mandatory unless SSO or token is used");
        } else {
            scanConfig.setUsername(username);
            scanConfig.setPassword(password);
        }
    }

    private String validatePassword(String password) {
        return (StringUtils.isNotEmpty(password) && password.startsWith("''")) ? password.substring(2) : password;
    }

    private String getSastOrOsaServerUrl() throws CLIParsingException {
        String rawUrl = getRequiredParam(commandLine, SERVER_URL, null);
        return normalizeUrl(rawUrl);
    }

    private void checkForAmbiguousArgs() throws CLIParsingException {
        String message = null;
        if (commandLine.hasOption(OSA_ENABLED) && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s and %s arguments cannot be specified together.", OSA_ENABLED, SCA_ENABLED);
        } else if (command == Command.SCA_SCAN && commandLine.hasOption(OSA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", OSA_ENABLED, command);
        } else if (command == Command.OSA_SCAN && commandLine.hasOption(SCA_ENABLED)) {
            message = String.format("%s argument cannot be used with %s command", SCA_ENABLED, command);
        }

        if (message != null) {
            throw new BadOptionCombinationException(message);
        }
    }

    private void setScanReports(CxScanConfig scanConfig) {
        String reportPath = getReportPath(PDF_REPORT);
        if (reportPath != null) {
            scanConfig.addPDFReport(reportPath);
        }

        reportPath = getReportPath(XML_REPORT);
        if (reportPath != null) {
            scanConfig.addXMLReport(reportPath);
        }

        reportPath = getReportPath(CSV_REPORT);
        if (reportPath != null) {
            scanConfig.addCSVReport(reportPath);
        }

        reportPath = getReportPath(RTF_REPORT);
        if (reportPath != null) {
            scanConfig.addRTFReport(reportPath);
        }
    }

    private String getReportPath(String optionName) {
        String rawValue = commandLine.getOptionValue(optionName);
        String result = null;
        if (rawValue != null) {
            result = rawValue.replace("..\\", "")
                    .replace("..//", "");
        }
        return result;
    }

    private CxScanConfig setSASTThresholds(CxScanConfig scanConfig) {
        String sastHigh = commandLine.getOptionValue(SAST_HIGH);
        String sastMedium = commandLine.getOptionValue(SAST_MEDIUM);
        String sastLow = commandLine.getOptionValue(SAST_LOW);

        scanConfig.setSastThresholdsEnabled(false);
        if (!Strings.isNullOrEmpty(sastHigh)) {
            scanConfig.setSastHighThreshold(Integer.valueOf(sastHigh));
            scanConfig.setSastThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(sastMedium)) {
            scanConfig.setSastMediumThreshold(Integer.valueOf(sastMedium));
            scanConfig.setSastThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(sastLow)) {
            scanConfig.setSastLowThreshold(Integer.valueOf(sastLow));
            scanConfig.setSastThresholdsEnabled(true);
        }

        return scanConfig;
    }

    private void setDependencyScanThresholds(CxScanConfig scanConfig) {
        String high = getSharedDependencyScanOption(scanConfig, OSA_HIGH, SCA_HIGH);
        String medium = getSharedDependencyScanOption(scanConfig, OSA_MEDIUM, SCA_MEDIUM);
        String low = getSharedDependencyScanOption(scanConfig, OSA_LOW, SCA_LOW);

        scanConfig.setOsaThresholdsEnabled(false);
        if (!Strings.isNullOrEmpty(high)) {
            scanConfig.setOsaHighThreshold(Integer.valueOf(high));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(medium)) {
            scanConfig.setOsaMediumThreshold(Integer.valueOf(medium));
            scanConfig.setOsaThresholdsEnabled(true);
        }
        if (!Strings.isNullOrEmpty(low)) {
            scanConfig.setOsaLowThreshold(Integer.valueOf(low));
            scanConfig.setOsaThresholdsEnabled(true);
        }
    }

    /**
     * Get one of the command line options that are shared between OSA and SCA and differ only
     * in prefix ("osa" or "sca").
     */
    private String getSharedDependencyScanOption(CxScanConfig scanConfig, String osaOption, String scaOption) {
        String result = null;
        if (scanConfig.isOsaEnabled()) {
            result = commandLine.getOptionValue(osaOption);
        } else if (scanConfig.isAstScaEnabled()) {
            result = commandLine.getOptionValue(scaOption);
        }
        return result;
    }

    private ScannerType getDependencyScannerType() {
        ScannerType result;
        if (command == Command.OSA_SCAN || command == Command.ASYNC_OSA_SCAN || commandLine.hasOption(OSA_ENABLED)) {
            result = ScannerType.OSA;
        } else if (command == Command.SCA_SCAN || command == Command.ASYNC_SCA_SCAN || commandLine.hasOption(SCA_ENABLED)) {
            result = ScannerType.AST_SCA;
        } else {
            result = null;
        }
        return result;
    }

    private static int getLastIndexOfTeam(String fullPath, boolean isScaScan) throws CLIParsingException {
        int lastIdxOfBackSlash = fullPath.lastIndexOf("\\");
        int lastIdxOfSlash = fullPath.lastIndexOf("/");
        if (lastIdxOfBackSlash == -1 && lastIdxOfSlash == -1) {
            if (isScaScan) {
                return -1; // so it takes the name from index 0, which is the start if the project name
            }
            throw new CLIParsingException(String.format("[CxConsole]  Invalid project path specified: %s", fullPath));
        }
        return lastIdxOfBackSlash != -1 ? lastIdxOfBackSlash : lastIdxOfSlash;
    }

    private static String extractProjectName(String fullPath, boolean isScaScan) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath, isScaScan);
        return fullPath.substring(lastIdx + 1);
    }

    private static String extractTeamPath(String fullPath) throws CLIParsingException {
        if (Strings.isNullOrEmpty(fullPath)) {
            throw new CLIParsingException("[CxConsole] No project path was specified");
        }
        int lastIdx = getLastIndexOfTeam(fullPath, false);
        return fullPath.substring(0, lastIdx);
    }

    public static void printConfig(CommandLine commandLine) {
        log.info("-----------------------------------------------------------------------------------------");
        log.info("CxConsole Configuration: ");
        log.info("--------------------");
        for (Option param : commandLine.getOptions()) {
            String name = param.getLongOpt() != null ? param.getLongOpt() : param.getOpt();
            String value;
            if (param.getOpt().equalsIgnoreCase(Parameters.USER_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(SCA_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(LOCATION_PASSWORD) ||
                    param.getOpt().equalsIgnoreCase(TOKEN)) {
                value = "********";
            } else if (param.getOpt().equalsIgnoreCase(USER_NAME) ||
                    param.getOpt().equalsIgnoreCase(SCA_USERNAME) ||
                    param.getOpt().equalsIgnoreCase(LOCATION_USER)) {
                value = param.getValue();
                log.debug("{}: {}", name, value);
                value = DigestUtils.sha256Hex(param.getValue());
            } else if (param.hasArg()) {
                value = param.getValue();
            } else {
                value = "true";
            }
            log.info("{}: {}", name, value);
        }
        log.info("--------------------\n\n");
    }

    public static void printHelp(Command mode) {
        if (mode == null) {
            return;
        }

        String footerFormat = "\nUsage example: %s\n\n(c) 2019 CheckMarx.com LTD, All Rights Reserved\n";
        String helpHeader = mode.getDescription();
        String helpFooter = String.format(footerFormat, mode.getUsageExamples());

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(120, mode.value(), helpHeader, Command.getOptions(), helpFooter, true);
    }

    private String getOptionalParam(String commandLineKey, String fallbackProperty) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        String propertyValue = props.getProperty(fallbackProperty);
        return isNotEmpty(commandLineValue) ? commandLineValue : propertyValue;
    }

    private String getParamWithDefault(String commandLineKey, String fallbackProperty, boolean useConfigAsCode) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        String propertyValue = props.getProperty(fallbackProperty);
        if (useConfigAsCode)
            return isNotEmpty(commandLineValue) && !commandLine.hasOption(CONFIG_AS_CODE) ? propertyValue + ", " + commandLineValue : propertyValue;
        return isNotEmpty(commandLineValue) ? propertyValue + ", " + commandLineValue : propertyValue;
    }

    private String getIncExcParamWithDefault(String commandLineKey) {
        String commandLineValue = commandLine.getOptionValue(commandLineKey);
        if (!commandLine.hasOption(CONFIG_AS_CODE) && isNotEmpty(commandLineValue)
                && Stream.of(commandLineValue.split(",")).map(String::trim).anyMatch(val -> !val.startsWith("!"))
                && !commandLineValue.contains("**/*")) {
            return ", **/*";
        }
        return "";
    }

    private static String getRequiredParam(CommandLine cmdLine, String cmdLineOptionName, @Nullable String fallbackProperty)
            throws CLIParsingException {
        String result = cmdLine.getOptionValue(cmdLineOptionName);
        if (Strings.isNullOrEmpty(result) && fallbackProperty != null) {
            result = props.getProperty(fallbackProperty);
        }

        if (Strings.isNullOrEmpty(result)) {
            String message;
            if (fallbackProperty != null) {
                message = String.format("%s command line option or %s configuration setting must be specified.",
                        cmdLineOptionName, fallbackProperty);
            } else {
                message = String.format("%s command line option must be specified.", cmdLineOptionName);
            }
            throw new CLIParsingException(message);
        }
        return result;
    }

    private static String normalizeUrl(String rawValue) {
        return rawValue.startsWith("http") ? rawValue : "http://" + rawValue;
    }

    private ProxyConfig genProxyConfig() {
        final String HTTP_HOST = System.getProperty("http.proxyHost");
        final String HTTP_PORT = System.getProperty("http.proxyPort");
        final String HTTP_USERNAME = System.getProperty("http.proxyUser");
        final String HTTP_PASSWORD = System.getProperty("http.proxyPassword");

        final String HTTPS_HOST = System.getProperty("https.proxyHost");
        final String HTTPS_PORT = System.getProperty("https.proxyPort");
        final String HTTPS_USERNAME = System.getProperty("https.proxyUser");
        final String HTTPS_PASSWORD = System.getProperty("https.proxyPassword");

        ProxyConfig proxyConfig = null;
        try {
            if (isNotEmpty(HTTP_HOST) && isNotEmpty(HTTP_PORT)) {
                proxyConfig = new ProxyConfig();
                proxyConfig.setUseHttps(false);
                proxyConfig.setHost(HTTP_HOST);
                proxyConfig.setPort(Integer.parseInt(HTTP_PORT));
                if (isNotEmpty(HTTP_USERNAME) && isNotEmpty(HTTP_PASSWORD)) {
                    proxyConfig.setUsername(HTTP_USERNAME);
                    proxyConfig.setPassword(HTTP_PASSWORD);
                }
            } else if (isNotEmpty(HTTPS_HOST) && isNotEmpty(HTTPS_PORT)) {
                proxyConfig = new ProxyConfig();
                proxyConfig.setUseHttps(true);
                proxyConfig.setHost(HTTPS_HOST);
                proxyConfig.setPort(Integer.parseInt(HTTPS_PORT));
                if (isNotEmpty(HTTPS_USERNAME) && isNotEmpty(HTTPS_PASSWORD)) {
                    proxyConfig.setUsername(HTTPS_USERNAME);
                    proxyConfig.setPassword(HTTPS_PASSWORD);
                }
            }
        } catch (Exception e) {
            log.error("Fail to set custom proxy", e);
        }

        return proxyConfig;
    }

    private boolean testConnection(CxScanConfig scanConfig) throws CLIParsingException {
        if (command.equals(Command.TEST_CONNECTION)) {
            if (commandLine.getOptionValue(SERVER_URL) != null) {
                scanConfig.setUrl(getSastOrOsaServerUrl());
                setSastOrOsaCredentials(scanConfig);
                scanConfig.setUseSSOLogin(commandLine.hasOption(IS_SSO));
            } else {
                setScaSpecificConfig(scanConfig);
            }
            return true;
        }
        return false;
    }
}
