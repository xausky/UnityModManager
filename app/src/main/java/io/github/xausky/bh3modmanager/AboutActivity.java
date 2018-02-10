package io.github.xausky.bh3modmanager;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Created by xausky on 18-2-10.
 */

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView content = findViewById(R.id.about_context);
        content.setText(Html.fromHtml(getString(R.string.about_context_text)));
        content.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
