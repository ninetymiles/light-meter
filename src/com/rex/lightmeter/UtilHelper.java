package com.rex.lightmeter;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.util.Log;


/*
 * Helper class for sending email or other separately functions
 */
public class UtilHelper {

	private static final String TAG = "RexLog";
	private static final boolean DEBUG = true;
	
	public static void sendEmail(Context context) {
		String defaultAddr = "timonlio@gmail.com";
		String defaultSubject = "Rax LightMeter";
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
		
		String defaultClientDevice = Build.MANUFACTURER + " - " + Build.MODEL + " - " + Build.PRODUCT + " / " + Build.VERSION.RELEASE;
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
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			context.startActivity(intent);
		} catch(Exception ex) {
			if (DEBUG) Log.w(TAG, "UtilHelper::sendEmail ex:" + ex.toString() + " will try ACTION_SEND");
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] { defaultAddr });
			intent.putExtra(Intent.EXTRA_SUBJECT, defaultSubject);
			intent.putExtra(Intent.EXTRA_TEXT, defaultBody);
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
	
	public static void rateThisApp(Context context) {
		String appURI = "market://details?id=" + context.getPackageName();
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appURI));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}
}
