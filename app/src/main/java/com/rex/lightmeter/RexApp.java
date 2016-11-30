package com.rex.lightmeter;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

@ReportsCrashes(
mailTo = "timonlio@gmail.com",
applicationLogFile = "/mnt/sdcard/rex.log",
customReportContent = { ReportField.APP_VERSION_NAME,
    ReportField.APP_VERSION_CODE,
    ReportField.ANDROID_VERSION,
    ReportField.PHONE_MODEL,
    ReportField.CUSTOM_DATA,
    ReportField.STACK_TRACE,
    ReportField.LOGCAT },
mode = ReportingInteractionMode.DIALOG,
resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few second    s
resDialogText = R.string.crash_dialog_text,
resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource a    s a label
resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
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

        if (! BuildConfig.DEBUG) {
            ACRA.init(this); // This line triggers the initialization of ACRA
        }

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
            rollingPolicy.setFileNamePattern(Environment.getExternalStorageDirectory() + File.separator + sExternalLogFile + ".%d{yyyy-MM-dd}");
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
