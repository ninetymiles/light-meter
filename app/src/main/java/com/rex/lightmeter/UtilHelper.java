package com.rex.lightmeter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

/*
 * Helper class for sending email or other separately functions
 */
public class UtilHelper {

    private static final Logger sLogger = LoggerFactory.getLogger("RexLog");

    public static void sendEmail(Context context) {
        String defaultAddr = "timonlio@gmail.com";
        String defaultSubject = "Rex LightMeter";
        String defaultBody = "\n\n";
        String defaultClientVersion = "";
        try {
            String pkgName = context.getPackageName();
            PackageManager pkgManager = context.getPackageManager();
            PackageInfo packageInfo = pkgManager.getPackageInfo(pkgName, 0);
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkgName, 0);
            String appLabel = pkgManager.getApplicationLabel(appInfo).toString();
            String version = String.format("%1$s v%2$s r%3$s",
                    appLabel,
                    packageInfo.versionName,
                    packageInfo.versionCode);

            defaultSubject = appLabel;
            defaultClientVersion = version;
        } catch (NameNotFoundException e) {}
        defaultBody += String.format(context.getString(R.string.contact_mail_default_body_version), defaultClientVersion);
        defaultBody += "\n";

        String defaultClientDevice = Build.MANUFACTURER + " " + Build.MODEL + "/" + Build.VERSION.RELEASE + " " + Build.PRODUCT + " (" + Build.DEVICE + " - " + Build.HARDWARE + ")";
        defaultBody += String.format(context.getString(R.string.contact_mail_default_body_device), defaultClientDevice);
        defaultBody += "\n";

        Locale locale = Locale.getDefault();
        String defaultClientLanguage = locale.getLanguage() + "-" + locale.getCountry();

        defaultBody += String.format(context.getString(R.string.contact_mail_default_body_lang), defaultClientLanguage);
        defaultBody += "\n";

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto: " + defaultAddr));
            intent.putExtra(Intent.EXTRA_SUBJECT, defaultSubject);
            intent.putExtra(Intent.EXTRA_TEXT, defaultBody);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory(), RexApp.sExternalLogFile)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(intent);
        } catch(Exception ex) {
            sLogger.warn("Failed to send mail by SENDTO\n", ex);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { defaultAddr });
            intent.putExtra(Intent.EXTRA_SUBJECT, defaultSubject);
            intent.putExtra(Intent.EXTRA_TEXT, defaultBody);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory(), RexApp.sExternalLogFile)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.contact_mail_chooser_title)));
        }
    }

    public static void shareThisApp(Context context) {
        String productName = context.getString(R.string.app_name);
        String defaultLink = "http://market.android.com/details?id=" + context.getPackageName();
        String defaultBody = String.format(context.getString(R.string.share_this_content), defaultLink);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain"); // Can show "Facebook" or "Twitter"
        intent.putExtra(Intent.EXTRA_SUBJECT, String.format(context.getString(R.string.share_this_subject), productName));
        intent.putExtra(Intent.EXTRA_TEXT,  defaultBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_this_chooser_title)));
    }
}
