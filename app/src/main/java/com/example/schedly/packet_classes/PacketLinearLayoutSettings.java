package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.cardview.widget.CardView;

import com.example.schedly.R;

import java.util.HashMap;

public class PacketLinearLayoutSettings extends CardView {
    private View view;
    private HashMap<String, CardView> mLinearLayoutHashMap = new HashMap<>();
    private final String[] mDaysOfTheWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final Integer[] mIDs = {R.id.frag_CWHours_CV_Monday, R.id.frag_CWHours_CV_Tuesday, R.id.frag_CWHours_CV_Wednesday, R.id.frag_CWHours_CV_Thursday, R.id.frag_CWHours_CV_Friday, R.id.frag_CWHours_CV_Saturday, R.id.frag_CWHours_CV_Sunday};

    public PacketLinearLayoutSettings(Context context, View _view) {
        super(context);
        this.view = _view;
        initializeMap();
    }


    private void initializeMap() {
        int _counter;
        for(_counter = 0; _counter < 7; _counter++) {
            mLinearLayoutHashMap.put(mDaysOfTheWeek[_counter], (CardView) this.view.findViewById(mIDs[_counter]));
        }
    }
}
