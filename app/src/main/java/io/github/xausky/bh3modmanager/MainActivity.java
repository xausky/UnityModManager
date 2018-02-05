package io.github.xausky.bh3modmanager;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    private MainService service;
    private ModsAdapter adapter = null;
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
}
