package com.example.schedly.adapter;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.example.schedly.R;

public class HelperPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private Dialog mDialog;

    public HelperPagerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        int resID = 0;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        switch (position) {
            case 0:
                resID = R.layout.dialog_first_login_helper;
                break;
            case 1:
                resID = R.layout.dialog_first_login_helper_page2;
                break;
            case 2:
                resID = R.layout.dialog_first_login_helper_sms;
                break;
            case 3:
                resID = R.layout.dialog_first_login_helper_sms_page2;
                break;
        }
        ViewGroup layout = (ViewGroup) inflater.inflate(resID, collection, false);
        setButtonClose(layout, position);
        collection.addView(layout);
        return layout;
    }

    private void setButtonClose(ViewGroup layout, int position) {
        if(position == 3) {
            Button _btnClose = layout.findViewById(R.id.dialog_FLHelper_BUT_Close);
            _btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDialog.dismiss();
                }
            });
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout)object);
    }

    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }
}
