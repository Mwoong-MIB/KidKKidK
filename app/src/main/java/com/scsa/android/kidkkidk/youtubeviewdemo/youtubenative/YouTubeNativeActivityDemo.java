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

import android.app.PendingIntent;
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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import com.scsa.android.kidkkidk.youtubeviewdemo.R;



public class YouTubeNativeActivityDemo extends AppCompatActivity {

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
}
