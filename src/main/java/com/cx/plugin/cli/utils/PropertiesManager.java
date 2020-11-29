package com.cx.plugin.cli.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by idanA on 11/5/2018.
 */
public class PropertiesManager {

    private static Logger log = LoggerFactory.getLogger(PropertiesManager.class);

    /*
     * Property keys
     */
    public static final String KEY_PROGRESS_INTERVAL = "scan.job.progress.interval";
    public static final String KEY_OSA_PROGRESS_INTERVAL = "scan.osa.job.progress.interval";
    public static final String KEY_RETRIES = "scan.job.connection.retries";
    public static final String REPORT_TIMEOUT = "scan.job.report.timeout";
    public static final String KEY_EXCLUDED_FOLDERS = "scan.zip.ignored.folders";
    public static final String KEY_EXCLUDED_FILES = "scan.zip.ignored.files";
    public static final String KEY_OSA_INCLUDED_FILES = "scan.osa.include.files";
    public static final String KEY_OSA_EXCLUDED_FILES = "scan.osa.exclude.files";
    public static final String KEY_OSA_EXTRACTABLE_INCLUDE_FILES = "scan.osa.extractable.include.files";
    public static final String KEY_OSA_SCAN_DEPTH = "scan.osa.extractable.depth";
    public static final String KEY_MAX_ZIP_SIZE = "scan.zip.max_size";
    public static final String KEY_DEF_PROJECT_NAME = "scan.default.projectname";
    public static final String KEY_VERSION = "cxconsole.version";
    public static final String KEY_USE_KERBEROS_AUTH = "use_kerberos_authentication";
    public static final String KEY_KERBEROS_USERNAME = "kerberos.username";

    static final String KEY_SCA_API_URL = "scan.sca.api.url";
    static final String KEY_SCA_ACCESS_CONTROL_URL = "scan.sca.accesscontrol.url";
    static final String KEY_SCA_WEB_APP_URL = "scan.sca.webapp.url";
    static final String KEY_SCA_INCLUDED_FILES = "scan.sca.include.files";
    static final String KEY_SCA_EXCLUDED_FILES = "scan.sca.exclude.files";
    static final String KEY_SCA_PROGRESS_INTERVAL = "scan.sca.job.progress.interval";

    private final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private String userDir = System.getProperty("user.dir");

    private String configDirRelativePath = "config";
    private String configFile = "cx_console.properties";

    private String defaultPath = userDir + SEPARATOR + configDirRelativePath + SEPARATOR + configFile;
    private Properties applicationProperties;

    private static PropertiesManager props;

    private PropertiesManager() {
    }

    private PropertiesManager(String defConfig) {
        applicationProperties = new Properties();
        loadProperties(defConfig);
    }

    public static PropertiesManager getProps(String configFilePath) {
        return props == null ? new PropertiesManager(configFilePath) : props;
    }
    public static PropertiesManager getProps(String configFilePath,Logger logger) {
        log=logger;
        return props == null ? new PropertiesManager(configFilePath) : props;
    }
    private void loadProperties(String confPath) {
        try {
            if (confPath != null && loadFromConfigParam(confPath)) {
                log.info("Config file location: {}", confPath);
                return;
            }

            if (!loadConfigFromFile(defaultPath) || applicationProperties.isEmpty()) {
                log.warn("Error occurred during loading configuration file. Default configuration values will be loaded.");
                loadDefaults();
            }

            log.info("Default configuration file location: {}", defaultPath);
        } catch (Exception ex) {
            log.warn("Error occurred during loading configuration file.");
        }
    }

    private boolean loadFromConfigParam(String confPath) {
        try {
            confPath = Paths.get(confPath).toFile().getCanonicalPath();
        } catch (Exception ex) {
            log.warn("Error occurred during loading configuration file. The Config path is invalid.");
            return false;
        }
        return loadConfigFromFile(confPath);
    }

    private boolean loadConfigFromFile(String path) {
        boolean ret = false;
        if (new File(path).exists()) {
            try (FileInputStream in = new FileInputStream(path)) {
                applicationProperties.load(in);

                ret = true;
            } catch (Exception e) {
                log.error("Error occurred during loading CxConsole properties.");
            }
        } else {
            log.error("The specified configuration path: [ {} ] does not exist.",path);
        }
        return ret;
    }

    private void loadDefaults() {
        applicationProperties.put(REPORT_TIMEOUT, "30");
        applicationProperties.put(KEY_PROGRESS_INTERVAL, "10");
        applicationProperties.put(KEY_OSA_PROGRESS_INTERVAL, "4");
        applicationProperties.put(KEY_RETRIES, "3");
        applicationProperties.put(KEY_EXCLUDED_FOLDERS, "_cvs, .svn, .hg, .git, .bzr, bin, obj, backup, node_modules");
        applicationProperties.put(KEY_EXCLUDED_FILES, "!**/*.DS_Store, !**/*.ipr, !**/*.iws, !**/*.bak, !**/*.tmp, !**/*.aac, !**/*.aif, !**/*.iff, !**/*.m3u, !**/*.mid, !**/*.mp3, !**/*.mpa, !**/*.ra, !**/*.wav, !**/*.wma, !**/*.3g2, !**/*.3gp, !**/*.asf, !**/*.asx, !**/*.avi, !**/*.flv, !**/*.mov, !**/*.mp4, !**/*.mpg, !**/*.rm, !**/*.swf, !**/*.vob, !**/*.wmv, !**/*.bmp, !**/*.gif, !**/*.jpg, !**/*.png, !**/*.psd, !**/*.tif, !**/*.jar, !**/*.zip, !**/*.rar, !**/*.exe, !**/*.dll, !**/*.pdb, !**/*.7z, !**/*.gz, !**/*.tar.gz, !**/*.tar, !**/*.ahtm, !**/*.ahtml, !**/*.fhtml, !**/*.hdm, !**/*.hdml, !**/*.hsql, !**/*.ht, !**/*.hta, !**/*.htc, !**/*.htd, !**/*.htmls, !**/*.ihtml, !**/*.mht, !**/*.mhtm, !**/*.mhtml, !**/*.ssi, !**/*.stm, !**/*.stml, !**/*.ttml, !**/*.txn, !**/*.class, !**/*.iml, !Checkmarx/Reports/*.*");
        applicationProperties.put(KEY_MAX_ZIP_SIZE, "200");
        applicationProperties.put(KEY_DEF_PROJECT_NAME, "console.project");
        applicationProperties.put(KEY_OSA_INCLUDED_FILES, "**/**");
        applicationProperties.put(KEY_OSA_EXCLUDED_FILES, "");
        applicationProperties.put(KEY_OSA_EXTRACTABLE_INCLUDE_FILES, "*.zip, *.war, *.ear, *.tgz");
        applicationProperties.put(KEY_OSA_SCAN_DEPTH, "4");
        applicationProperties.put(KEY_USE_KERBEROS_AUTH, "false");
        applicationProperties.put(KEY_SCA_API_URL, "https://api-sca.checkmarx.net");
        applicationProperties.put(KEY_SCA_ACCESS_CONTROL_URL, "https://platform.checkmarx.net");
        applicationProperties.put(KEY_SCA_WEB_APP_URL, "https://sca.checkmarx.net");

        File propsFile = new File(defaultPath);
        if (!propsFile.exists()) {
            File configDir = new File(userDir + SEPARATOR + configDirRelativePath);
            if (!configDir.exists()) {
                configDir.mkdir();
            }
            try (FileOutputStream fOut = new FileOutputStream(propsFile)) {
                applicationProperties.store(fOut, "");
            } catch (IOException e) {
                log.warn("Cannot create configuration file");
            }
        }
    }

    public String getProperty(String key) {
        Object value = applicationProperties.get(key);
        return value == null ? null : value.toString();
    }

    public Integer getIntProperty(String key) {
        Object value = applicationProperties.get(key);
        Integer intValue = null;
        if (value != null) {
            try {
                intValue = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Can't parse string to int value: {}", e.getMessage());
            }
        }
        return intValue;
    }

    public Long getLongProperty(String key) {
        Object value = applicationProperties.get(key);
        Long longValue = null;
        if (value != null) {
            try {
                longValue = Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Can't parse string to long value: {}", e.getMessage());
            }
        }
        return longValue;
    }

}
