package com.score_test;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IconTextView extends LinearLayout {

    private ImageView mIcon;
    private TextView mText01;  // ,mText02,mText03;

    // 생성자
    public IconTextView(Context context, IconTextItem aItem) {
        super(context);

        Typeface typeFace = Typeface.createFromAsset(context.getAssets(), "fonts/dx경필명조Bold.ttf");   // asset > fonts 폴더 내의 폰트 파일 적용


        // 인플레이터로 화면 설정, listitem이랑 연결
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sheet_music_item, this, true);

        // 아이콘 세팅
        mIcon = (ImageView)findViewById(R.id.sheet_music_title_icon);
        mIcon.setImageDrawable(aItem.getIcon());

        mText01 = (TextView)findViewById(R.id.sheet_music_title);
        mText01.setText(aItem.getData(0));
        mText01.setTypeface(typeFace);
        //mText01.setTypeface(Typeface.createFromFile("C:\\Graduation_ndk_version\\Graduation_Program\\app\\src\\main\\assets\\fonts\\dx경필명조.ttf"));
        //글꼴 오류   ->  여기선 getAssets() 가 안되서 저렇게 가져와야된다....

        //mText02 = (TextView)findViewById(R.id.dataItem02);
        //mText02.setText(aItem.getData(1));

        //mText03 = (TextView)findViewById(R.id.dataItem03);
        //mText03.setText(aItem.getData(2));
    }

    // 텍스트 삽입
    public void setText(int index, String data){
        if (index == 0){
            mText01.setText(data);
        }
        /*
        else if (index == 1){
            mText02.setText(data);
        }
        else if (index == 2){
            mText03.setText(data);
        }
        */
        else {
            throw new IllegalArgumentException();
        }
    }

    // 아이콘 삽입
    public void setIcon(Drawable icon){
        mIcon.setImageDrawable(icon);
    }
}