package io.stubbs.captcha;

import com.google.common.base.Stopwatch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

@Slf4j
public class FunctionUtils {

    static public void logStopwatch(Logger log, Stopwatch w) {
        logStopwatch(null, log, w);
    }

    static public void logStopwatch(String logMessage, Logger shadowLogger, Stopwatch w) {
        Logger logger = (shadowLogger == null) ? FunctionUtils.log : shadowLogger;
        if (logMessage == null)
            logger.info("Took: {}", w);
        else
            logger.info("{} took: {}", logMessage, w);
    }

    @SneakyThrows
    static public void randomPause(String message) {
        FunctionUtils.randomPause(message, ofMillis(500), ofMillis(500));
    }

    @SneakyThrows
    static public void randomPause(String message, Duration atLeast, Duration withinRandomDuration) {
        int v = (int) (withinRandomDuration.toMillis() * Math.random());
        long sleep = atLeast.toMillis() + v;
        String template = (StringUtils.isBlank(message)) ? "Sleeping for {}" : message + ". Sleeping for {}...";
        log.info(template, sleep);
        Thread.sleep(sleep);
    }

    @SneakyThrows
    public static <R> R time(String logMessage, Supplier<R> f) {
        return time(logMessage, null, f);
    }

    @SneakyThrows
    public static <R> R time(Supplier<R> f) {
        return time(null, f);
    }

    @SneakyThrows
    public static <R> R time(String logMessage, Logger log, Supplier<R> f) {
        Logger logger = (log == null) ? FunctionUtils.log : log;
        logMessage = (logMessage == null) ?
                "Action..." :
                logMessage;
        logger.info(logMessage);
        Stopwatch stopwatch = Stopwatch.createStarted();
        R apply = f.get();
        logStopwatch(logMessage, log, stopwatch);
        return apply;
    }
}
