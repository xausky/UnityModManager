package io.github.xausky.bh3modmanager.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-3-3.
 */

public class SettingFragment extends PreferenceFragment {
    public static final String SETTINGS_PREFERENCE_NAME = "settings";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.preferences);
        this.getPreferenceManager().setSharedPreferencesName(SETTINGS_PREFERENCE_NAME);
    }
}