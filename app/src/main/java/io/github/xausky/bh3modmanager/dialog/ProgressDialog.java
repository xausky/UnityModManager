package io.github.xausky.bh3modmanager.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-3-7.
 */

public class ProgressDialog extends BottomSheetDialog {

    public ProgressDialog(@NonNull Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
        this.setContentView(view);
        this.setCancelable(false);
    }

}
