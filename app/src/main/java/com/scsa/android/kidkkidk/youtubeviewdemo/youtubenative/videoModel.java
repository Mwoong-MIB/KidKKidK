package com.scsa.android.kidkkidk.youtubeviewdemo.youtubenative;

import java.util.ArrayList;

public class videoModel extends ArrayList<String> {

    static public ArrayList<String> videoA = new ArrayList<>();

    @Override
    public boolean add(String url) {
        videoA.add(url);

        return false;
    }

    public static ArrayList<String> getInstance(){
        if (videoA == null){
            videoA = new ArrayList<String>();
        }
        return videoA;
    }
}
