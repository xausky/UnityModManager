package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.adapter.AttachesAdapter;
import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.dialog.ApplicationChooseDialog;

/**
 * Created by xausky on 18-3-7.
 */

public class VisibilityFragment extends BaseFragment  implements ApplicationChooseDialog.OnApplicationChooseDialogResultListener{
    private static final String ALL_APPLICATION_PACKAGE_REGEX = "^.*$";
    private Context context;
    private ApplicationChooseDialog dialog;
    private View view;
    private RecyclerView attaches;
    private VisibilityAdapter adapter;
    private HomeFragment homeFragment;

    @Override
    public BaseFragment setBase(Context base) {
        adapter = new VisibilityAdapter(base);
        return super.setBase(base);
    }

    public int getItemCount(){
        return adapter.getItemCount();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.visibility_fragment, container, false);
        homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
        context = inflater.getContext();
        dialog = new ApplicationChooseDialog(context, this, ALL_APPLICATION_PACKAGE_REGEX, false, false);
        dialog.setListener(this);
        attaches = (RecyclerView) view.findViewById(R.id.visibility_list);
        adapter.setRecyclerView(attaches);
        return view;
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        if(VirtualCore.get().isStartup()) {
            VisibilityFragment.this.dialog.show();
        } else {
            Toast.makeText(context, R.string.not_available_non_virtual, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        dialog.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void OnApplicationChooseDialogResult(String packageName, String apkPath) {
        dialog.hide();
        adapter.addVisibleOutsidePackage(packageName);
    }
}
