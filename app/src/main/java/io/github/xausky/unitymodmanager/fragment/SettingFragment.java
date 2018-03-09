package io.github.xausky.unitymodmanager.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import io.github.xausky.unitymodmanager.R;

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