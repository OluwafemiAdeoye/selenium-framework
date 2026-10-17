package com.framework.selenium.utils;

import com.framework.selenium.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.FormatKeys.MimeTypeKey;
import static org.monte.media.VideoFormatKeys.*;

/**
 * Records screen video during test execution using Monte Media Library.
 * Video is retained only on test failure to conserve disk space.
 *
 * <p>Thread-safety: each test thread should create its own {@code VideoRecorder}
 * instance and store it in a {@code ThreadLocal} (handled by {@code BaseTest}).
 * Internally, all mutable state is guarded by {@code synchronized} blocks so
 * that concurrent calls to {@link #startRecording} / {@link #stopRecording}
 * on the same instance are safe.</p>
 *
 * <p>Note: Requires a display. Not compatible with headless mode.
 * In CI, either disable via config or run under Xvfb.</p>
 */
public class VideoRecorder {

    private static final Logger log = LogManager.getLogger(VideoRecorder.class);

    private ScreenRecorder screenRecorder;
    private String testName;
    private volatile boolean recording;

    /**
     * Begins screen recording for the given test.
     * If recording is already in progress, the call is ignored.
     *
     * @param testName human-readable test name used in log messages
     */
    public synchronized void startRecording(String testName) {
        if (recording) {
            log.warn("Recording already in progress for: {} — ignoring duplicate start", this.testName);
            return;
        }

        this.testName = testName;
        try {
            Path videoDir = Paths.get(FrameworkConstants.VIDEOS_DIR);
            Files.createDirectories(videoDir);

            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();

            this.screenRecorder = new ScreenRecorder(gc,
                    gc.getBounds(),
                    new Format(MediaTypeKey, FormatKeys.MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO,
                            EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                            QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60),
                    new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO,
                            EncodingKey, "black", FrameRateKey, Rational.valueOf(30)),
                    null,
                    videoDir.toFile());

            screenRecorder.start();
            recording = true;
            log.info("Video recording started for test: {}", testName);
        } catch (Exception e) {
            recording = false;
            screenRecorder = null;
            log.error("Failed to start video recording for: {}", testName, e);
        }
    }

    /**
     * Stops recording if — and only if — a recording was successfully started.
     * Safe to call multiple times or even if {@link #startRecording} was never called;
     * subsequent calls are no-ops.
     *
     * @param keepFile {@code true} to retain the video (typically on failure),
     *                 {@code false} to delete it (typically on pass)
     */
    public synchronized void stopRecording(boolean keepFile) {
        if (!recording) {
            log.debug("stopRecording called but no active recording — nothing to do (test: {})", testName);
            return;
        }

        try {
            screenRecorder.stop();
            List<File> createdFiles = screenRecorder.getCreatedMovieFiles();
            if (!keepFile) {
                createdFiles.forEach(file -> {
                    if (file.delete()) {
                        log.debug("Deleted video (test passed): {}", file.getName());
                    }
                });
            } else {
                createdFiles.forEach(file ->
                        log.info("Video retained (test failed): {}", file.getAbsolutePath()));
            }
        } catch (IOException e) {
            log.error("Failed to stop video recording for: {}", testName, e);
        } finally {
            recording = false;
            screenRecorder = null;
        }
    }

    /**
     * @return {@code true} if a recording is currently in progress
     */
    public boolean isRecording() {
        return recording;
    }
}
