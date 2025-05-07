package com.cx.plugin.cli.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class VersionChecker {
    private static final Logger log = LogManager.getLogger(VersionChecker.class);
    private static final String API_URL = "https://api.github.com/repos/checkmarx-ltd/CLI/releases/latest";

    public static void checkForUpdates(String currentVersion) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            log.warn("Failed to determine the current version.");
            return;
        }
        currentVersion = sanitizeVersion(currentVersion);

        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null) {
                log.warn("Failed to fetch the latest version.");
                return;
            }
            latestVersion = sanitizeVersion(latestVersion);

            log.info("Current version: {}", currentVersion);
            log.info("Latest version: {}", latestVersion);

            int comparison = compareVersions(currentVersion, latestVersion);
            if (comparison < 0) {
                log.warn("A new version is available: {}. Please visit checkmarx.com/plugins and download the latest version.", latestVersion);
            } else {
                log.info("You are running the latest version.");
            }
        } catch (IOException e) {
            log.error("Error checking for updates: {}", e.getMessage());
        }
    }

    private static String fetchLatestVersion() throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.error("Failed to fetch latest release. HTTP Response Code: {}", responseCode);
            return null;
        }

        Scanner scanner = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        JSONObject json = new JSONObject(response.toString());
        return json.optString("tag_name"); // GitHub releases store the version in "tag_name"
    }

    private static int compareVersions(String v1, String v2) {
        String[] v1Parts = v1.split("\\.");
        String[] v2Parts = v2.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int num1 = (i < v1Parts.length) ? Integer.parseInt(v1Parts[i]) : 0;
            int num2 = (i < v2Parts.length) ? Integer.parseInt(v2Parts[i]) : 0;

            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    private static String sanitizeVersion(String version) {
        if (version == null) return "0.0.0";
        Pattern pattern = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)");
        Matcher matcher = pattern.matcher(version);
        return matcher.find() ? matcher.group(1) : "0.0.0"; // Extracts only major.minor.patch
    }
}
