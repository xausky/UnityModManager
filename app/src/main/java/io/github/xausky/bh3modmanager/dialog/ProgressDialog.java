package io.github.xausky.bh3modmanager.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-3-7.
 */

public class ProgressDialog extends BottomSheetDialog {

    public ProgressDialog(@NonNull Context context, View parent) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, (ViewGroup) parent, false);
        this.setContentView(view);
        this.setCancelable(false);
    }

}
