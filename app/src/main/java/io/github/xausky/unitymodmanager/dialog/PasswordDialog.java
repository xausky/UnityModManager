package io.github.xausky.unitymodmanager.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
        setButton(BUTTON_POSITIVE, context.getString(R.string.confirm), (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.skip), (OnClickListener) null);
    }
    public void setPositiveListener(View.OnClickListener listener){
        getButton(BUTTON_POSITIVE).setOnClickListener(listener);
    }
    public void setNegativeListener(View.OnClickListener listener){
        getButton(BUTTON_NEGATIVE).setOnClickListener(listener);
    }
}
