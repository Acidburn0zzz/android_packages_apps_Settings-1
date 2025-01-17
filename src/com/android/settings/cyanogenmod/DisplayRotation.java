/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.System;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class DisplayRotation extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "DisplayRotation";

    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String ROTATION_0_PREF = "display_rotation_0";
    private static final String ROTATION_90_PREF = "display_rotation_90";
    private static final String ROTATION_180_PREF = "display_rotation_180";
    private static final String ROTATION_270_PREF = "display_rotation_270";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";

    private ListPreference mAccelerometer;
    private CheckBoxPreference mRotation0Pref;
    private CheckBoxPreference mRotation90Pref;
    private CheckBoxPreference mRotation180Pref;
    private CheckBoxPreference mRotation270Pref;
    private CheckBoxPreference mSwapVolumeButtons;

    public static final int ROTATION_0_MODE = 1;
    public static final int ROTATION_90_MODE = 2;
    public static final int ROTATION_180_MODE = 4;
    public static final int ROTATION_270_MODE = 8;

    private ContentObserver mAccelerometerRotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateAccelerometerRotationCheckbox();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean hasRotationLock = this.getResources().getBoolean(com.android
                .internal.R.bool.config_hasRotationLockSwitch);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.display_rotation);

        PreferenceScreen prefSet = getPreferenceScreen();

        mAccelerometer = (ListPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setOnPreferenceChangeListener(this);
        
        mRotation0Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_0_PREF);
        mRotation90Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_90_PREF);
        mRotation180Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_180_PREF);
        mRotation270Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_270_PREF);

        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                        ROTATION_0_MODE|ROTATION_90_MODE|ROTATION_270_MODE);

        mRotation0Pref.setChecked((mode & ROTATION_0_MODE) != 0);
        mRotation90Pref.setChecked((mode & ROTATION_90_MODE) != 0);
        mRotation180Pref.setChecked((mode & ROTATION_180_MODE) != 0);
        mRotation270Pref.setChecked((mode & ROTATION_270_MODE) != 0);

        mSwapVolumeButtons = (CheckBoxPreference) prefSet.findPreference(KEY_SWAP_VOLUME_BUTTONS);
        if (mSwapVolumeButtons != null) {
            if (!Utils.hasVolumeRocker(getActivity())) {
                prefSet.removePreference(mSwapVolumeButtons);
            } else {
                int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                        Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
                mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
            }
        }

        if (hasRotationLock) {
            // Disable accelerometer, but leave others enabled
            mAccelerometer.setEnabled(false);
            mSwapVolumeButtons.setDependency(null);
            mRotation0Pref.setDependency(null);
            mRotation90Pref.setDependency(null);
            mRotation180Pref.setDependency(null);
            mRotation270Pref.setDependency(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
    }

    private void updateAccelerometerRotationCheckbox() {
        mAccelerometer.setValue(
            !RotationPolicy.isRotationLocked(getActivity()) && 
                Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_LOCKSCREEN, 0) == 1 ? "2" : 

                !RotationPolicy.isRotationLocked(getActivity()) ? "1" : "0"
        );

        mRotation0Pref.setEnabled( !RotationPolicy.isRotationLocked(getActivity()) );
        mRotation90Pref.setEnabled( !RotationPolicy.isRotationLocked(getActivity()) );
        mRotation180Pref.setEnabled( !RotationPolicy.isRotationLocked(getActivity()) );
        mRotation270Pref.setEnabled( !RotationPolicy.isRotationLocked(getActivity()) );
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAccelerometer) {
            int value = Integer.parseInt((String) newValue);

            RotationPolicy.setRotationLockForAccessibility(getActivity(), value > 0 ? false : true);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_LOCKSCREEN, value > 1 ? 1 : 0);

            return true;
        }

        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRotation0Pref ||
                preference == mRotation90Pref ||
                preference == mRotation180Pref ||
                preference == mRotation270Pref) {
            int mode = 0;
            if (mRotation0Pref.isChecked())
                mode |= ROTATION_0_MODE;
            if (mRotation90Pref.isChecked())
                mode |= ROTATION_90_MODE;
            if (mRotation180Pref.isChecked())
                mode |= ROTATION_180_MODE;
            if (mRotation270Pref.isChecked())
                mode |= ROTATION_270_MODE;
            if (mode == 0) {
                mode |= ROTATION_0_MODE;
                mRotation0Pref.setChecked(true);
            }
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_ANGLES, mode);
            return true;
        } else if (preference == mSwapVolumeButtons) {
            Context context = getActivity().getApplicationContext();
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                    mSwapVolumeButtons.isChecked()
                    ? (Utils.isTablet(context) ? 2 : 1)
                    : 0);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
