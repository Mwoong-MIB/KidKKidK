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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.scsa.android.kidkkidk.youtubeview.models.YouTubePlayerType;
import com.scsa.android.kidkkidk.youtubeviewdemo.youtubenative.YouTubeNativeActivityDemo;
import com.scsa.android.kidkkidk.youtubeviewdemo.youtubenative.videoModel;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements BeaconConsumer{


    private BeaconManager beaconManager;
    private double DISTANCE = 2.0D;

    private Region region = new Region("altbeacon", null, null, null);

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private boolean needBLERequest = true;
    private String TAG = "MainActivity";

    private int PERMISSIONS_CODE = 100;

    // 모든 퍼미션 관련 배열
    private final String[] requiredPermissions = new String[]{"android.permission.ACCESS_FINE_LOCATION"};

    SoundPool pool;
    int alertMusic;
    AlertDialog ctrlODialog = null;
    Button checkDistanceBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button youtubeNativeFragmentButton = findViewById(R.id.youtubeNativeFragment);
        checkDistanceBtn = findViewById(R.id.checkDistance);

//
        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            ActivityCompat.requestPermissions(this, this.requiredPermissions, this.PERMISSIONS_CODE);
        }

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add((new BeaconParser()).setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        bluetoothManager = (android.bluetooth.BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        alertMusic = pool.load(this, R.raw.alert, 1);

        checkDistanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DISTANCE = 2.0D;

                startScan();

                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("거리 설정");
                alert.setMessage("몇 m 떨어지면 알려드릴까요?");

                final EditText input = new EditText(MainActivity.this);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        DISTANCE = Double.parseDouble(value);
                    }
                });

                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                alert.show();
            }
        });


        youtubeNativeFragmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, YouTubeNativeActivityDemo.class);
                intent.putExtra("playerType", YouTubePlayerType.AUTO);
                startActivity(intent);
            }
        });


    }

    private final void startScan() {
        if (!isEnableBLEService()) {
            requestEnableBLE();
            Log.d(this.TAG, "startScan: 블루투스가 켜지지 않았습니다.");

        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            ActivityCompat.requestPermissions(this, this.requiredPermissions, this.PERMISSIONS_CODE);
        }

        Log.d(this.TAG, "startScan: beacon Scan start");
        beaconManager.bind(this);
    }

    private final boolean isEnableBLEService() {
        if (!bluetoothAdapter.isEnabled()) {
            return false;
        }
        return true;
    }

    private final void requestEnableBLE() {
        Intent callBLEEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        this.requestBLEActivity.launch(callBLEEnableIntent);
        Log.d(this.TAG, "requestEnableBLE: ");
    }

    ActivityResultLauncher requestBLEActivity = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback() {
                @Override
                public void onActivityResult(Object result) {
                    if (isEnableBLEService()) {
                        needBLERequest = false;
                        startScan();
                    }
                }
            });

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.PERMISSIONS_CODE) {
            if (grantResults.length != 0) {
                String[] perArr = permissions;
                int perLength = permissions.length;

                for (int i = 0; i < perLength; ++i) {
                    String permission = perArr[i];
                    if (grantResults[i] != 0) {
                        Log.e(this.TAG, permission + " 권한 획득에 실패하였습니다.");
                        this.finish();
                    }
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addMonitorNotifier((MonitorNotifier)(new MonitorNotifier() {

            public void didEnterRegion(@Nullable Region region) {
                try {
                    Log.e("INFO", "비콘 발견 : "+region.toString());
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            public void didExitRegion(@Nullable Region region) {
                try {
                    Log.e("INFO", "비콘을 찾을 수 없습니다. ..");
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            public void didDetermineStateForRegion(int i, @Nullable Region region) {
            }
        }));


        beaconManager.addRangeNotifier((RangeNotifier)(new RangeNotifier() {

            public final void didRangeBeaconsInRegion(Collection beacons, Region region) {

                Iterator beaIter = beacons.iterator();

                while(beaIter.hasNext()) {
                    final Beacon beacon = (Beacon)beaIter.next();

                    Log.i("INFO", "distance ==> " + beacon.getDistance());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double distance = beacon.getDistance();
                            Log.e("INFO", "distance ==> " + (distance));

                            boolean alert_flag = false;

                            if(distance > DISTANCE) alert_flag = true;
                            else if(distance < DISTANCE) alert_flag = false;

                            if(alert_flag){
                                if(ctrlODialog == null){
                                    pool.play(alertMusic, 1, 1, 0, 1, 1);  // 소리 내기
                                    // 다이얼로그 팝업
                                    AlertDialog.Builder bDialog = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Holo_DialogWhenLarge_NoActionBar);
                                    bDialog.setMessage("거리가 멀어졌습니다.")
                                            .setTitle("우리 아이 지키미 경보")
                                            // 다이얼로그 인증 취소 버튼
                                            .setPositiveButton("경보 해제", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    Log.i("Dialog", "원격 제어 닫기");
                                                    Toast.makeText(MainActivity.this, "경보 해제", Toast.LENGTH_LONG).show();
                                                    ctrlODialog.dismiss();
                                                    ctrlODialog = null;
                                                    pool.stop(alertMusic);
                                                }
                                            })
                                            .setCancelable(false); // 백버튼으로 팝업창이 닫히지 않도록 한다.
                                    ctrlODialog = bDialog.show();
                                }
                            }
                            if(!alert_flag){
                                if(ctrlODialog != null){
                                    ctrlODialog.dismiss();
                                    ctrlODialog = null;
                                    pool.stop(alertMusic);
                                }
                            }
                        }
                    });
                }
            }
        }));

        try {
            beaconManager.startMonitoringBeaconsInRegion(this.region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    protected void onDestroy() {
        super.onDestroy();

        try {
            beaconManager.stopMonitoringBeaconsInRegion(region);
            beaconManager.stopRangingBeaconsInRegion(this.region);
            beaconManager.unbind((BeaconConsumer)this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


}
