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
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    private static final int CHOOSE_APK_REQUEST_CODE = 0x8848;
    private MainService service;
    private ModsAdapter adapter = null;
    private Dialog dialog = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button launch = findViewById(R.id.launch);
        ListView mods = findViewById(R.id.mods);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_APK_REQUEST_CODE) {
            Uri uri = data.getData();
            Log.d(service.LOG_TAG, "choose uri: " + uri + ", getPath:" + FileUtils.resolveFilePath(this, uri));
            service.install(FileUtils.resolveFilePath(this, uri));
            dialog.dismiss();
        }
    }

    public void chooseApk(Dialog dialog){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CHOOSE_APK_REQUEST_CODE);
        this.dialog = dialog;
    }
}
