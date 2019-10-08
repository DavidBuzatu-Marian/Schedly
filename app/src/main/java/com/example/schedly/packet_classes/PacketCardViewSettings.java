package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.example.schedly.R;
import com.example.schedly.model.DaysOfWeek;

import java.util.HashMap;

public class PacketCardViewSettings extends CardView {
    private View mView;
    private Context mContext;
    private RelativeLayout mRootRelativeLayout;

    public PacketCardViewSettings(Context context, View _view) {
        super(context);
        mContext = context;
        mView = _view;
        createCards();
    }


    private void createCards() {
        DaysOfWeek _previousDay = DaysOfWeek.MON;
        mRootRelativeLayout = mView.findViewById(R.id.frag_CWHours_RL_root);
        for (DaysOfWeek _day : DaysOfWeek.values()) {
            if (!_day.geteDisplayName().equals("All")) {
                CardView _cardView = new CardView(mView.getContext());
                RelativeLayout.LayoutParams _layoutParamsCV = new RelativeLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );
                RelativeLayout.LayoutParams _layoutParamsCB = newRLLayoutParams();
                RelativeLayout.LayoutParams _layoutParamsTV = newRLLayoutParams();
                RelativeLayout.LayoutParams _layoutParamsSPS = newRLLayoutParams();
                RelativeLayout.LayoutParams _layoutParamsSPE = newRLLayoutParams();
                RelativeLayout _relativeLayoutInCard = new RelativeLayout(mView.getContext());
                _relativeLayoutInCard.setLayoutParams(_layoutParamsCV);
                _relativeLayoutInCard.setPadding(2, 4, 2, 4);

                if (_day.geteDisplayName().equals("Monday")) {
                    _layoutParamsCV.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                } else {
                    _layoutParamsCV.addRule(RelativeLayout.BELOW, _previousDay.getCardViewId());
                }
                addCV(_layoutParamsCV, _cardView, _relativeLayoutInCard, _day);
                addCB(_layoutParamsCB, _relativeLayoutInCard, _day);
                addTV(_layoutParamsTV, _relativeLayoutInCard, _day);
                addSpinnerEnd(_layoutParamsSPE, _relativeLayoutInCard, _day);
                addSpinnerStart(_layoutParamsSPS, _relativeLayoutInCard, _day);
                _previousDay = _day;
            }
        }
    }

    private void addSpinnerStart(RelativeLayout.LayoutParams layoutParamsSPS, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        Spinner _spinnerStart = new Spinner(mView.getContext(), Spinner.MODE_DIALOG);
        _spinnerStart.setId(day.geteSpinnerStartID());
        _spinnerStart.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleStart));
        relativeLayoutInCard.addView(_spinnerStart, layoutParamsSPS);
        layoutParamsSPS.addRule(RelativeLayout.START_OF, day.geteSpinnerEndID());
        layoutParamsSPS.addRule(RelativeLayout.CENTER_VERTICAL);
        layoutParamsSPS.setMargins(0, 0, 0, 0);
        _spinnerStart.setLayoutParams(layoutParamsSPS);
    }

    private void addSpinnerEnd(RelativeLayout.LayoutParams layoutParamsSPE, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        Spinner _spinnerEnd = new Spinner(mView.getContext(), Spinner.MODE_DIALOG);
        _spinnerEnd.setId(day.geteSpinnerEndID());
        _spinnerEnd.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleEnd));
        relativeLayoutInCard.addView(_spinnerEnd, layoutParamsSPE);
        layoutParamsSPE.addRule(RelativeLayout.ALIGN_PARENT_END);
        layoutParamsSPE.addRule(RelativeLayout.CENTER_VERTICAL);
        _spinnerEnd.setLayoutParams(layoutParamsSPE);
    }

    private void addTV(RelativeLayout.LayoutParams layoutParamsTV, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        TextView _TVDayLabel = new TextView(mView.getContext());
        _TVDayLabel.setText(day.geteDisplayName().substring(0, 3));
        _TVDayLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        relativeLayoutInCard.addView(_TVDayLabel, layoutParamsTV);
        layoutParamsTV.addRule(RelativeLayout.END_OF, day.geteCheckBoxID());
        layoutParamsTV.setMargins(12, 0, 0, 6);
        layoutParamsTV.addRule(RelativeLayout.CENTER_VERTICAL);
        _TVDayLabel.setLayoutParams(layoutParamsTV);
    }

    private void addCB(RelativeLayout.LayoutParams layoutParamsCB, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        CheckBox _checkBox = new CheckBox(mView.getContext());
        _checkBox.setId(day.geteCheckBoxID());
        _checkBox.setText(R.string.act_SWHours_CB_FreeDay);
        relativeLayoutInCard.addView(_checkBox, layoutParamsCB);
        layoutParamsCB.addRule(RelativeLayout.ALIGN_PARENT_START);
        layoutParamsCB.setMargins(0, 0, 12, 0);
        layoutParamsCB.addRule(RelativeLayout.CENTER_VERTICAL);
        _checkBox.setLayoutParams(layoutParamsCB);
    }

    private void addCV(RelativeLayout.LayoutParams layoutParamsCV, CardView cardView, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        layoutParamsCV.setMargins(64, 9, 64, 9);
        cardView.setRadius(32);
        cardView.setCardElevation(6);
        cardView.setId(day.getCardViewId());
        cardView.setLayoutParams(layoutParamsCV);

        mRootRelativeLayout.addView(cardView);
        cardView.addView(relativeLayoutInCard);
    }

    private RelativeLayout.LayoutParams newRLLayoutParams() {
        return new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
    }

}
