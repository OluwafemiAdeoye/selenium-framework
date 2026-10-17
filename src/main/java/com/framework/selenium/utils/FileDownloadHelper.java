package com.framework.selenium.utils;

import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Utilities for verifying file downloads.
 * Works with the download directory configured in DriverFactory.
 */
public final class FileDownloadHelper {

    private static final Logger log = LogManager.getLogger(FileDownloadHelper.class);

    private FileDownloadHelper() {
    }

    /**
     * Returns the configured download directory path.
     */
    public static Path getDownloadDir() {
        String dir = ConfigManager.get("download.dir", FrameworkConstants.DOWNLOADS_DIR);
        return Paths.get(dir).toAbsolutePath();
    }

    /**
     * Waits for a file with the given name to appear in the download directory.
     * Polls every 500ms up to the specified timeout.
     */
    public static boolean waitForFileDownload(String fileName, Duration timeout) {
        Path filePath = getDownloadDir().resolve(fileName);
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        log.info("Waiting for file download: {}", filePath);

        while (System.currentTimeMillis() < endTime) {
            if (Files.exists(filePath) && !isPartialDownload(filePath)) {
                log.info("File downloaded successfully: {}", filePath);
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("File download timed out: {}", fileName);
        return false;
    }

    /**
     * Waits for any new file to appear in the download directory.
     * Returns the file path if found, null if timed out.
     */
    public static Path waitForAnyNewFile(long fileCountBefore, Duration timeout) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        Path downloadDir = getDownloadDir();

        while (System.currentTimeMillis() < endTime) {
            try (Stream<Path> files = Files.list(downloadDir)) {
                long currentCount = files
                        .filter(p -> !isPartialDownload(p))
                        .count();
                if (currentCount > fileCountBefore) {
                    // Return the newest file
                    try (Stream<Path> sorted = Files.list(downloadDir)) {
                        return sorted
                                .filter(p -> !isPartialDownload(p))
                                .max((a, b) -> {
                                    try {
                                        return Files.getLastModifiedTime(a)
                                                .compareTo(Files.getLastModifiedTime(b));
                                    } catch (IOException e) {
                                        return 0;
                                    }
                                })
                                .orElse(null);
                    }
                }
            } catch (IOException e) {
                log.error("Error listing download directory", e);
                return null;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the count of files currently in the download directory.
     */
    public static long getFileCount() {
        try (Stream<Path> files = Files.list(getDownloadDir())) {
            return files.filter(p -> !isPartialDownload(p)).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Checks if the given file exists in the download directory.
     */
    public static boolean isFileDownloaded(String fileName) {
        return Files.exists(getDownloadDir().resolve(fileName));
    }

    /**
     * Returns the file size in bytes, or -1 if the file doesn't exist.
     */
    public static long getFileSize(String fileName) {
        Path filePath = getDownloadDir().resolve(fileName);
        try {
            return Files.exists(filePath) ? Files.size(filePath) : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Deletes a file from the download directory.
     */
    public static boolean deleteDownloadedFile(String fileName) {
        try {
            return Files.deleteIfExists(getDownloadDir().resolve(fileName));
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Clears all files in the download directory.
     */
    public static void clearDownloadDir() {
        try (Stream<Path> files = Files.list(getDownloadDir())) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Failed to delete: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.error("Failed to clear download directory", e);
        }
    }

    /**
     * Checks if a file is still being downloaded (partial download).
     * Chrome uses .crdownload, Firefox uses .part extensions.
     */
    private static boolean isPartialDownload(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".crdownload") || name.endsWith(".part") || name.endsWith(".tmp");
    }
}
