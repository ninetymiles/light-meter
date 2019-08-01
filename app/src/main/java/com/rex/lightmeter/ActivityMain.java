package com.rex.lightmeter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityMain extends AppCompatActivity {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    private long mExitTime;

    private OrientationHelper mOrientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger.trace("");

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("CONF_ENABLE_KEEP_SCREEN_ON", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_main);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.addTab(bar.newTab().setText("Incident").setTabListener(new UiTabListener(new FragmentIncident())));
        if (checkCameraHardware(this)) {
            bar.addTab(bar.newTab().setText("Reflection").setTabListener(new UiTabListener(new FragmentReflect())));
        }
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("TAB", 0));
        }

        mOrientation = new OrientationHelper(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLogger.trace("");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLogger.trace("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLogger.trace("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLogger.trace("");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("CONF_ENABLE_KEEP_SCREEN_ORIENTATION", true)) {
            mOrientation.lock();
        } else {
            mOrientation.unlock();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLogger.trace("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mLogger.trace("itemId:{}", item.getItemId());
        switch (item.getItemId()) {
        case R.id.menu_setting:
            startActivity(new Intent(this, ActivitySettings.class));
            break;
        case R.id.menu_feedback:
            UtilHelper.sendEmail(this);
            break;
        case R.id.menu_share:
            UtilHelper.shareThisApp(this);
            break;
        case R.id.menu_about:
            startActivity(new Intent(this, ActivityAbout.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mLogger.trace("");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mLogger.trace("");
        //outState.putDouble("LUX", mMeter.getLux());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mLogger.trace("");
        //mMeter.setLux(savedInstanceState.getDouble("LUX"));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        mLogger.trace("");
        if (System.currentTimeMillis() - mExitTime > 2000) {
            Toast.makeText(getApplicationContext(), R.string.toast_quit, Toast.LENGTH_SHORT).show();
            mExitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private class UiTabListener implements TabListener {
        private Fragment mFragment;
        public UiTabListener(Fragment fragment) {
            mFragment = fragment;
        }
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.replace(android.R.id.content, mFragment);
        }
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            //ft.remove(mFragment);
        }
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            mLogger.trace("");
        }
    }
}
