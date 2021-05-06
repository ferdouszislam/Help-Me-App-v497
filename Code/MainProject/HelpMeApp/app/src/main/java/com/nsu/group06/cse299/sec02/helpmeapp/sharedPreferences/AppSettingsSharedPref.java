package com.nsu.group06.cse299.sec02.helpmeapp.sharedPreferences;

import android.content.Context;

public class AppSettingsSharedPref extends SharedPrefsUtil{

    public static AppSettingsSharedPref build(Context context) {

        return new AppSettingsSharedPref(SharedPrefKeysUtil.APP_SETTINGS_ID, context);
    }

    private AppSettingsSharedPref(String mSharedPreferenceId, Context mContext) {
        super(mSharedPreferenceId, mContext);
    }

    public void setEmergencyModeState(boolean state) {

        saveBooleanData(SharedPrefKeysUtil.EMERGENCY_MODE_KEY, state);
    }

    public boolean getEmergencyModeState(boolean defaultState) {

        return getBooleanData(SharedPrefKeysUtil.EMERGENCY_MODE_KEY, defaultState);
    }

}
