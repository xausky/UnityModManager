package io.github.xausky.bh3modmanager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {
    public static final int CHOOSE_APK_REQUEST_CODE = 0x8848;
    public static final int CHOOSE_MOD_REQUEST_CODE = 0x8849;
    private MainService service;
    private ModsAdapter adapter = null;
    private Dialog dialog = null;
    private Switch force = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button launch = findViewById(R.id.launch);
        ListView mods = findViewById(R.id.mods);
        force = findViewById(R.id.switch_froce);
        adapter = new ModsAdapter(this);
        mods.setAdapter(adapter);
        service = new MainService(this);
        service.setAdapter(adapter);
        service.setLaunch(launch);
        service.start();
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.launch();
            }
        });
        force.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                service.forceChange(isChecked);
            }
        });
    }

    public void setForce(boolean force){
        this.force.setChecked(force);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.main_menu_reinstall:
                service.chooseInstall(true);
                break;
            case R.id.main_menu_import:
                chooseFile(null, CHOOSE_MOD_REQUEST_CODE);
                break;
            case R.id.main_menu_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK){
            return;
        }
        String path = FileUtils.resolveFilePath(this, data.getData());
        Log.d(MainService.LOG_TAG, "Choose Path:" + path);
        switch (requestCode){
            case CHOOSE_APK_REQUEST_CODE:
                service.install(path, null);
                try {
                    dialog.dismiss();
                }catch (Exception e){

                }
                break;
            case CHOOSE_MOD_REQUEST_CODE:
                service.importMod(path);
                break;
        }
    }

    public void chooseFile(Dialog dialog, int requestCode){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
        this.dialog = dialog;
    }
}
