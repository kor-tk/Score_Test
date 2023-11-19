package com.score_test;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class IconTextListAdapter extends BaseAdapter {

    private Context mContext;
    private List<IconTextItem> mItems = new ArrayList<IconTextItem>();

    public IconTextListAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void addItem(IconTextItem it){
        mItems.add(it);
    }

    public void setmItems(List<IconTextItem> mItems) {
        this.mItems = mItems;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    public boolean areAllltemsSelectable(){
        return false;
    }

    public boolean isSelectable(int position){
        try {
            return mItems.get(position).isSelectable();
        }
        catch (IndexOutOfBoundsException ex){
            return false;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 화면 구성하는 getView
    // convertView가 null이 아니면 뷰는 재활용하고 안의 데이터만 바꿔주어 퍼포먼스 향상
    // null인 경우에는 새로 객체 생성해줌
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IconTextView itemView;

        if (convertView == null) {
            itemView = new IconTextView(mContext, mItems.get(position));
        }
        else {
            itemView = (IconTextView)convertView;
            itemView.setIcon(mItems.get(position).getIcon());
            itemView.setText(0, mItems.get(position).getData(0));
        }
        return itemView;
    }
}