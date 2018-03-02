package io.github.xausky.bh3modmanager.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import io.github.xausky.bh3modmanager.R;
import mehdi.sakout.aboutpage.AboutPage;

/**
 * Created by xausky on 18-2-10.
 */

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.drawable.dummy_image)
                .setDescription(getString(R.string.about_context_text))
                .addEmail("xausky@163.com")
                .addWebsite("https://xausky.github.io/")
                .addGitHub("xausky")
                .create();
        setContentView(aboutPage);
    }
}
