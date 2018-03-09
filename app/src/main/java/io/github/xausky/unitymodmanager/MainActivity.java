package io.github.xausky.unitymodmanager;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

import io.github.xausky.unitymodmanager.fragment.BaseFragment;
import io.github.xausky.unitymodmanager.fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private FloatingActionButton actionButton;
    private NavigationView navigationView;
    private int currentNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        drawerLayout = findViewById(R.id.dl_main_drawer);
        navigationView = findViewById(R.id.nv_main_navigation);
        actionButton = findViewById(R.id.action_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseFragment fragment = (BaseFragment)BaseFragment.fragment(currentNavigation);
                try {
                    fragment.OnActionButtonClick();
                }catch (UnsupportedOperationException e){
                    Snackbar.make(view, "不支持的操作", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navigation(item.getItemId());
                item.setChecked(true);
                return true;
            }
        });
        navigation(R.id.nav_home);
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
                HomeFragment fragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
                boolean isInstall = VirtualCore.get().isAppInstalled(fragment.packageName);
                if(!isInstall){
                    Snackbar.make(actionButton, "客户端未安装，请先到主页安装客户端。", Snackbar.LENGTH_LONG).show();
                    break;
                }
                Intent intent = VirtualCore.get().getLaunchIntent(fragment.packageName, 0);
                VActivityManager.get().startActivity(intent, 0);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
