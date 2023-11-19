package com.score_test;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

public class IconTextItem {
    private Drawable mIcon;
    private String mData;
    private boolean mSelectable = true;

    // 파라미터값이 String으로 된 생성자
    public IconTextItem(Drawable mIcon, String obj01) {
        //                                String obj02, String obj03
        this.mIcon = mIcon;

        mData = obj01;
        //mData[1] = obj02;
        //mData[2] = obj03;
    }

    public boolean isSelectable() {
        return mSelectable;
    }

    public void setmSelectable(boolean mSelectable) {
        this.mSelectable = mSelectable;
    }

    // 문자열 데이터 반환
    public String getData() {
        return mData;
    }


    // String 반환 파리미터 인덱스 값
    public String getData(int index) {
        return mData;
    }


    public void setData(String obj) {
        mData = obj;
    }

    public void setIcon(Drawable Icon) {
        mIcon = Icon;
    }
    public Drawable getIcon() {
        return mIcon;
    }
}