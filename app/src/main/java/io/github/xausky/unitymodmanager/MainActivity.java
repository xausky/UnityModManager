package io.github.xausky.unitymodmanager;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

import java.io.File;

import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.fragment.BaseFragment;
import io.github.xausky.unitymodmanager.fragment.HomeFragment;
import io.github.xausky.unitymodmanager.fragment.ModFragment;
import io.github.xausky.unitymodmanager.utils.ModUtils;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private FloatingActionButton actionButton;
    private NavigationView navigationView;
    private int currentNavigation;
    private ProgressDialog dialog;
    private HomeFragment homeFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preferences = this.getSharedPreferences("default", MODE_PRIVATE);
        SharedPreferences visibilityPreferences = this.getSharedPreferences(VisibilityAdapter.VISIBILITY_SHARED_PREFERENCES_KEY, MODE_PRIVATE);
        if(preferences.getBoolean("first", true)){
            //微信和支付宝默认可见（用于氪金）
            visibilityPreferences.edit()
                    .putBoolean("com.eg.android.AlipayGphone", true)
                    .putBoolean("com.tencent.mm", true).apply();
            preferences.edit().putBoolean("first", false).apply();
        }
        BaseFragment.initialize(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        drawerLayout = (DrawerLayout) findViewById(R.id.dl_main_drawer);
        navigationView = (NavigationView) findViewById(R.id.nv_main_navigation);
        actionButton = (FloatingActionButton) findViewById(R.id.action_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseFragment fragment = (BaseFragment)BaseFragment.fragment(currentNavigation);
                try {
                    fragment.OnActionButtonClick();
                }catch (UnsupportedOperationException e){
                    Toast.makeText(MainActivity.this, "不支持的操作", Toast.LENGTH_SHORT).show();
                }
            }
        });
        actionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                BaseFragment fragment = (BaseFragment)BaseFragment.fragment(currentNavigation);
                try {
                    fragment.OnActionButtonLongClick();
                }catch (UnsupportedOperationException e){
                    Toast.makeText(MainActivity.this, "不支持的操作", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navigation(item.getItemId());
                item.setChecked(true);
                MainActivity.this.setTitle(getString(R.string.app_name) + "-" + item.getTitle());
                return true;
            }
        });
        navigation(R.id.nav_home);
        dialog = new ProgressDialog(this);
        dialog.setTitle(R.string.progress_dialog_title);
        dialog.setMessage(getString(R.string.progress_dialog_message));
        dialog.setCancelable(false);
        homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
        MainActivity.this.setTitle(getString(R.string.app_name) + "-" + getString(R.string.nav_home));
    }

    private void navigation(int item){
        Fragment fragment = BaseFragment.fragment(item);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        if(fragment instanceof BaseFragment){
            actionButton.setVisibility(((BaseFragment)fragment).actionButtonVisibility());
        } else {
            actionButton.setVisibility(View.INVISIBLE);
        }
        drawerLayout.closeDrawers();
        currentNavigation = item;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.menu_launch_game:
                launch();
                break;
        }
        return true;
    }

    public void launch(){
        if(homeFragment.apkPath == null || homeFragment.baseApkPath == null || !new File(homeFragment.baseApkPath).exists()){
            Toast.makeText(this, "请先到主页安装客户端，并且保证安装源不被卸载或者删除。", Toast.LENGTH_LONG).show();
        } else {
            new PatchApkTask(dialog).execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    static class PatchApkTask extends AsyncTask<Object, Object, Integer> {
        private ProgressDialog dialog;

        public PatchApkTask(ProgressDialog dialog) {
            super();
            this.dialog = dialog;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Integer doInBackground(Object... params) {
            int result = ModUtils.RESULT_STATE_OK;
            ModFragment modFragment = (ModFragment) BaseFragment.fragment(R.id.nav_mod);
            HomeFragment homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
            if (modFragment.isNeedPatch()) {
                result = modFragment.patch(homeFragment.apkPath, homeFragment.baseApkPath, homeFragment.rootModel);
            }
            if (result == ModUtils.RESULT_STATE_OK) {
                if(homeFragment.rootModel){
                    Intent intent = dialog.getContext().getPackageManager().getLaunchIntentForPackage(homeFragment.packageName);
                    dialog.getContext().startActivity(intent);
                } else {
                    Intent intent = VirtualCore.get().getLaunchIntent(homeFragment.packageName, 0);
                    VActivityManager.get().startActivity(intent, 0);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            dialog.hide();
            ModFragment modFragment = (ModFragment) BaseFragment.fragment(R.id.nav_mod);
            if (result == ModUtils.RESULT_STATE_OK) {
                modFragment.setNeedPatch(false);
            } else if (result == ModUtils.RESULT_STATE_INTERNAL_ERROR) {
                Toast.makeText(modFragment.getBase(), "安装模组失败", Toast.LENGTH_LONG).show();
            }
        }
    }
}
