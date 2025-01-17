/*
 * Copyright (C) 2012 The CyanogenMod project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.provider.Settings;
import android.provider.Settings.System;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_NAVIGATION_BAR_TOGGLE = "navigation_bar_toggle";
    private static final String KEY_NAVIGATION_BAR_HIDABLE = "navigation_bar_hidable";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_QUICK_SETTINGS = "quick_settings_panel";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";

    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private PreferenceScreen mPieControl;
    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;

    private boolean mIsPrimary;

    private CheckBoxPreference mNavBarToggle;
    private CheckBoxPreference mNavBarHidable;
    private PreferenceScreen mNavBar;
    private PreferenceScreen mNavRing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));

        boolean configNavBar = getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        boolean hasNavigationBar = false;
        boolean hasSystemNavBar = false;

        try {
            hasSystemNavBar = windowManager.hasSystemNavBar();
            hasNavigationBar = windowManager.hasNavigationBar();

        } catch (RemoteException e) {}

        // Determine which user is logged in
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mIsPrimary) {
            // Primary user only preferences
            // Battery lights
            mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
            if (mBatteryPulse != null) {
                if (getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                    prefScreen.removePreference(mBatteryPulse);
                } else {
                    updateBatteryPulseDescription();
                }
            }

            // Act on the above
            if (configNavBar || hasSystemNavBar) {
                prefScreen.removePreference(findPreference(KEY_HARDWARE_KEYS));
            }
            if (hasSystemNavBar) {
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_HIDABLE));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_TOGGLE));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_CATEGORY));

            } else {
                mNavBar = (PreferenceScreen) findPreference(KEY_NAVIGATION_BAR);
                mNavRing = (PreferenceScreen) findPreference(KEY_NAVIGATION_RING);
                mNavBarToggle = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_TOGGLE);
                mNavBarHidable = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_HIDABLE);

                mNavBarToggle.setChecked(
                    Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.NAV_BAR_VISABLE, hasNavigationBar ? 1 : 0) == 1
                );
                
                mNavBarHidable.setChecked(
                    Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.NAV_BAR_HIDABLE, hasNavigationBar ? 1 : 0) == 1
                );

                if (!hasNavigationBar) {
                    mNavBar.setEnabled(false);
                    mNavRing.setEnabled(false);
                } 
            }

        } else {
            // Secondary user is logged in, remove all primary user specific preferences
            prefScreen.removePreference(findPreference(KEY_BATTERY_LIGHT));
            prefScreen.removePreference(findPreference(KEY_HARDWARE_KEYS));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_CATEGORY));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_HIDABLE));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_TOGGLE));
            prefScreen.removePreference(findPreference(KEY_STATUS_BAR));
            prefScreen.removePreference(findPreference(KEY_QUICK_SETTINGS));
            prefScreen.removePreference(findPreference(KEY_POWER_MENU));
            prefScreen.removePreference(findPreference(KEY_NOTIFICATION_DRAWER));
        }

        // Preferences that applies to all users
        // Notification lights
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                prefScreen.removePreference(mNotificationPulse);
                mNotificationPulse = null;
            }
        }

        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);
        if (mPieControl != null && !hasNavigationBar) {
            // Remove on devices without a navbar to start with
            mPieControl.setEnabled(false);
            mPieControl = null;
        }

        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopNoNavbarPref = (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);

        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);

        // Hide no-op "Status bar visible" mode on devices without navbar
        try {
            if (WindowManagerGlobal.getWindowManagerService().hasNavigationBar()) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);
                prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
            } else {
                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                prefScreen.removePreference(mExpandedDesktopPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        // Don't display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));
    }

    @Override
    public void onResume() {
        super.onResume();

        // All users
        if (mNotificationPulse != null) {
            updateLightPulseDescription();
        }
        if (mPieControl != null) {
            updatePieControlDescription();
        }

        // Primary user only
        if (mIsPrimary && mBatteryPulse != null) {
            updateBatteryPulseDescription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            updateExpandedDesktop(expandedDesktopValue);
            return true;
        } else if (preference == mExpandedDesktopNoNavbarPref) {
            boolean value = (Boolean) objValue;
            updateExpandedDesktop(value ? 2 : 0);
            return true;
        }

        return false;
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
     }

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

    private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNavBarToggle) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.NAV_BAR_VISABLE, mNavBarToggle.isChecked() ? 1 : 0);

            return true;

        } else if (preference == mNavBarHidable) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.NAV_BAR_HIDABLE, mNavBarHidable.isChecked() ? 1 : 0);

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
