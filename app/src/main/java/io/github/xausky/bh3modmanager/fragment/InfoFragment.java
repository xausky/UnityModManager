package io.github.xausky.bh3modmanager.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.github.xausky.bh3modmanager.R;
import mehdi.sakout.aboutpage.AboutPage;

/**
 * Created by xausky on 18-3-3.
 */

public class InfoFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return new AboutPage(inflater.getContext())
                .setDescription(getString(R.string.info_context_text))
                .addEmail("xausky@163.com")
                .addWebsite("https://xausky.github.io/")
                .addGitHub("xausky")
                .create();
    }
}
