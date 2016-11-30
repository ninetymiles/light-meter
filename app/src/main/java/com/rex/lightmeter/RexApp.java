package com.rex.lightmeter;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Base64;

import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class RexApp extends Application {

    private final org.slf4j.Logger mLogger = LoggerFactory.getLogger("RexLog");

    public static final String sExternalLogFile = "rex.log";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    //.detectAll()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
////                    .detectAll()
//                    .detectLeakedClosableObjects()
//                    .detectLeakedSqlLiteObjects()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build());
        }

        mLogger.info("LogFile:{}", Environment.getExternalStorageDirectory() + File.separator + sExternalLogFile);
        if (! BuildConfig.DEBUG) {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.INFO);

            RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<ILoggingEvent>();
            rollingFileAppender.setContext(ctx);
            rollingFileAppender.setAppend(true);
            rollingFileAppender.setFile(Environment.getExternalStorageDirectory() + File.separator + sExternalLogFile);

            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
            rollingPolicy.setContext(ctx);
            rollingPolicy.setFileNamePattern(Environment.getExternalStorageDirectory()  + File.separator + "rex.%d{yyyy-MM-dd}.log");
            rollingPolicy.setMaxHistory(6);
            rollingPolicy.setParent(rollingFileAppender);
            rollingPolicy.start();

            rollingFileAppender.setRollingPolicy(rollingPolicy);

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(ctx);
            encoder.setPattern("%date %level/%logger [%thread] %class{0}::%method %msg%n");
            encoder.start();

            rollingFileAppender.setEncoder(encoder);
            rollingFileAppender.start();

            rootLogger.addAppender(rollingFileAppender);
        }
    }
}
