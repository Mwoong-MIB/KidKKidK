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
package com.scsa.android.kidkkidk.youtubeviewdemo.youtubenative;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import com.scsa.android.kidkkidk.youtubeviewdemo.MainActivity;
import com.scsa.android.kidkkidk.youtubeviewdemo.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;


public class YouTubeNativeActivityDemo extends AppCompatActivity {

    private BluetoothAdapter blueAdapter;
    private BluetoothDevice blueDevice;
    private BluetoothGatt blueGatt;
    private String bleRemoteConName;
    private Button connectRemoteBtn;

    UUID UUID_KEY_SERV = UUID
            .fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    UUID UUID_KEY_DATA = UUID
            .fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private BluetoothLeScanner leScanner;

    // 모든 퍼미션 관련 배열
    private int PERMISSIONS_CODE = 100;
    private final String[] requiredPermissions = new String[]{"android.permission.ACCESS_FINE_LOCATION"};

    static public final int ON_MODE = 1;
    static public final int OFF_MODE = 2;
    public int NFC_MODE_R = 0;

    NfcAdapter nfcAdapter;
    PendingIntent pIntent;
    IntentFilter[] filters;
    AlertDialog ctrlODialog;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.youtube_native_activity_demo);

        //
        blueAdapter = BluetoothAdapter.getDefaultAdapter();
        connectRemoteBtn = findViewById(R.id.connectRemote);
        connectRemoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        bleRemoteConName = "78:A5:04:8C:15:16";
                        showMessage(bleRemoteConName+ " 등록되었습니다...");
                        srtBleScan();
            }
        });
        leScanner = blueAdapter.getBluetoothLeScanner();

        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            ActivityCompat.requestPermissions(this, this.requiredPermissions, this.PERMISSIONS_CODE);
        }

        //
        Bundle extras = getIntent().getExtras();

        YouTubeNativeFragmentDemo youTubeNativeFragmentDemo = new YouTubeNativeFragmentDemo();
        youTubeNativeFragmentDemo.setArguments(extras);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, youTubeNativeFragmentDemo);
        fragmentTransaction.commit();


        NFC_MODE_R = OFF_MODE;


    // NFC 태그 확인
        // 다이얼로그 팝업
        AlertDialog.Builder oDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog);
        oDialog.setMessage("인증을 시작합니다." + "NFC를 태그해주세요.")
                .setTitle("NFC 태그 인증")
                // 다이얼로그 인증 취소 버튼
                .setPositiveButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Log.i("Dialog", "NFC 인증 취소");
                        Toast.makeText(getApplicationContext(), "NFC 인증 취소", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setCancelable(false); // 백버튼으로 팝업창이 닫히지 않도록 한다.
        ctrlODialog = oDialog.show();
        tagAuthWait();
    }

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.PERMISSIONS_CODE) {
            if (grantResults.length != 0) {
                String[] perArr = permissions;
                int perLength = permissions.length;

                for (int i = 0; i < perLength; ++i) {
                    String permission = perArr[i];
                    if (grantResults[i] != 0) {
                        Log.i("INFO", permission + " 권한 획득에 실패하였습니다.");
                        this.finish();
                    }
                }
            }
        }
    }

    // NFC 태그 대기
    private void tagAuthWait() {
        Toast.makeText(getApplicationContext(), "NFC 인증 대기 중", Toast.LENGTH_LONG).show();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter==null){
            Toast.makeText(this, "기기가 NFC를 지원하지 않습니다..", Toast.LENGTH_SHORT).show();
            finish();
        }
        //포그라운드 기능을 활성화시킬 해당 객체들을 생성.... =====================================
        Intent i = new Intent(this, this.getClass());
        //액티비티가 Top에 있을 때 인스턴스를 재사용한다...
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pIntent = PendingIntent.getActivity(this, 0, i, 0);

        IntentFilter tag_filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters = new IntentFilter[]{tag_filter};
    }

    @Override
    protected void onResume() {
        super.onResume();
        //활성화....
        nfcAdapter.enableForegroundDispatch(this, pIntent, filters, null);
        Log.i("INFO", "onRusme... 포그라운드 기능 활성화.....");
        if(!blueAdapter.isEnabled()){
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();
        Log.i("INFO", "onNewIntent called.......action : "+action);

        //infoTv.append("action : "+action+"\n");
        if(action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED) ||
                action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) ||
                action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)){
            //읽기모드....
            if(processingTag(intent)){
                if(NFC_MODE_R == OFF_MODE) {
                    NFC_MODE_R = ON_MODE;
                    startLockTask();
                }
                else {
                    NFC_MODE_R = OFF_MODE;
                    stopLockTask();
                }
                ctrlODialog.dismiss();
            }
            else{
                Toast.makeText(getApplicationContext(), "NFC 인증에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //기능 비활성화...
    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
        Log.i("INFO", "onPause... 포그라운드 기능 비 활성화 ========");
        stopBleScan();
    }

    private boolean processingTag(Intent intent){

        // 인텐트에서 NdefMessage 객체를 꺼낸다.
        Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage[] ndefArr = new NdefMessage[rawMsg.length];

        for(int i = 0; i < rawMsg.length; i++){
            ndefArr[i] = (NdefMessage) rawMsg[i];
        }

        NdefRecord record =ndefArr[0].getRecords()[0];

        byte[] typeArr = record.getType();
        String tagType = new String(typeArr);
        Log.i("INFO", "Tag data type : " + tagType);

        if(tagType.equals("T")){
            byte[] dataArr = record.getPayload();
            String tagData = new String(dataArr, 3, dataArr.length-3);
            if(tagData.equals("kidkAuth-01200t")){
                return true;
            }
            else return false;
        }
        else return false;
    }

    @Override
    public void onBackPressed() {
        if(NFC_MODE_R == ON_MODE){
            Toast.makeText(this, "NFC를 태그해 잠금을 해제해주세요.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    private void srtBleScan() {
        leScanner.startScan(sCallback);
        showMessage("검색을 시작합니다.........");
    }

    ScanCallback sCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            Log.e("INFO", "++++++ "+device.getAddress()+ "++++++++");
            if(device.getAddress().equals(bleRemoteConName)){
                blueDevice = device;
                //blueAdapter.stopLeScan(lecallBack);
                leScanner.stopScan(sCallback);
                //connect 작업 수행....
                blueGatt = blueDevice.connectGatt(YouTubeNativeActivityDemo.this, false, gattCallback);
            }
        }
    };

    private void stopBleScan() {
        leScanner.stopScan(sCallback);
        if(blueGatt!=null){
            blueGatt.disconnect();
            blueGatt = null;
        }
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // TODO Auto-generated method stub
            super.onConnectionStateChange(gatt, status, newState);
            // 연결되면...Ble sensor가 제공하는 모드 서비스 정보를 가져온다..
            if(newState== BluetoothProfile.STATE_CONNECTED){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessage("리모콘 장비와 연결되었습니다..");
                        blueGatt.discoverServices();
                    }
                });
            }
        }
        //
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // TODO Auto-generated method stub
            super.onServicesDiscovered(gatt, status);
            // Notification을 받을 수 있도록 센서를 활성화 시킨다..
            Log.e("INFO", "onServicesDiscovered called.....");
            enableKeySensor();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic value) {
            // TODO Auto-generated method stub
            super.onCharacteristicChanged(gatt, value);
            Log.e("INFO", "onCharacteristicChanged called.....");
            getCharacteristic(value);
        }

    };

    private void enableKeySensor() {

        BluetoothGattCharacteristic keyGattCData = null;
        keyGattCData = blueGatt.getService(UUID_KEY_SERV).getCharacteristic(UUID_KEY_DATA);
//		//Notification 활성화 시키는 코드
        blueGatt.setCharacteristicNotification(keyGattCData, true);

        BluetoothGattDescriptor descriptor = keyGattCData.getDescriptor(UUID
                .fromString("00002902-0000-1000-8000-00805f9b34fb"));

        if (descriptor != null) {
            byte[] val = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            blueGatt.writeDescriptor(descriptor);
        }
        Log.e("INFO", "enableKeySensor() called.....");
    }

    public void getCharacteristic(BluetoothGattCharacteristic ch) {

        if (blueAdapter == null || blueGatt == null || ch == null) {
            return;
        }
        UUID uuid = ch.getUuid();
        final byte[] rawValue = ch.getValue();
        final String value = Conversion.BytetohexString(rawValue, rawValue.length);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(value.equals("02")){
                    // 다이얼로그 팝업
                    AlertDialog.Builder bDialog = new AlertDialog.Builder(YouTubeNativeActivityDemo.this, android.R.style.Theme_Holo_DialogWhenLarge_NoActionBar);
                    bDialog.setMessage("동영상을 중지합니다.")
                            .setTitle("원격 제어")
                            // 다이얼로그 인증 취소 버튼
                            .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    Log.i("Dialog", "원격 제어 닫기");
                                    Toast.makeText(getApplicationContext(), "재생 중지", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            })
                            .setCancelable(false); // 백버튼으로 팝업창이 닫히지 않도록 한다.
                    ctrlODialog = bDialog.show();
                }else if(value.equals("01")){
                    ctrlODialog.dismiss();

                }else{
                }
            }
        });
    }

    public void listmake(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("유튜브 리스트 추가");
        alert.setMessage("추가할 Url을 입력해주세요.");

// Set an EditText view to get user input
        final EditText input = new EditText(YouTubeNativeActivityDemo.this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                //https://youtu.be/MQN49JJ7nL4
                // 특정단어(부분)만 자르기
                String target1 = "https://youtu.be/";
                int target_num1 = value.indexOf(target1);

                String tData;
                tData = value.substring(target_num1 + 17);

                ArrayList<String> videoIds = videoModel.getInstance();
                videoIds.add(tData);

            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
// Canceled.
                    }
                });
        alert.show();
    }

    private void showMessage(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
