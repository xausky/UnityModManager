package io.github.xausky.unitymodmanager.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import io.github.xausky.unitymodmanager.R;

/**
 * Created by xausky on 18-3-11.
 */

public class PasswordDialog extends AlertDialog {
    public EditText edit;
    public PasswordDialog(@NonNull Context context) {
        super(context);
        edit = new EditText(context);
        setView(edit);
        setTitle(context.getString(R.string.please_input_password));
        setButton(BUTTON_POSITIVE, "确定", (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, "跳过", (OnClickListener) null);
    }
    public void setPositiveListener(View.OnClickListener listener){
        getButton(BUTTON_POSITIVE).setOnClickListener(listener);
    }
    public void setNegativeListener(View.OnClickListener listener){
        getButton(BUTTON_NEGATIVE).setOnClickListener(listener);
    }
}
