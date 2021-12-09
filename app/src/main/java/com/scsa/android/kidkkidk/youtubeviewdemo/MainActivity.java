/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 *
 * Copyright (c) 2018 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.scsa.android.kidkkidk.youtubeviewdemo;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.scsa.android.kidkkidk.youtubeview.models.YouTubePlayerType;
import com.scsa.android.kidkkidk.youtubeviewdemo.remote.RemoteControlActivity;
import com.scsa.android.kidkkidk.youtubeviewdemo.youtubenative.YouTubeNativeActivityDemo;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button youtubeNativeFragmentButton = findViewById(R.id.youtubeNativeFragment);
        Button connectButton = findViewById(R.id.connectRemote);

        youtubeNativeFragmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, YouTubeNativeActivityDemo.class);
                intent.putExtra("playerType", YouTubePlayerType.AUTO);
                startActivity(intent);
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoteControlActivity.class);
                startActivity(intent);
            }
        });
    }
}
