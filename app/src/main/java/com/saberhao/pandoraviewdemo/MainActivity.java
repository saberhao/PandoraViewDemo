package com.saberhao.pandoraviewdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PandoraView pandoraView = findViewById(R.id.pandoraView);
        InputStream is = null;
        try {
            is = getResources().getAssets().open("world_map.jpg"); //宽图
            //is = getResources().getAssets().open("comic.jpg"); //长图
            pandoraView.setImage(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing here
                }
            }
        }
    }
}
