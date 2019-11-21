package com.davidbuzatu.schedly.packet_classes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.DaysOfWeek;

public class PacketCardView extends CardView {

    private Activity mActivity;
    private RelativeLayout mRootRelativeLayout;
    private final String TAG = "PacketCardView";

    public PacketCardView(Context context, Activity _activity) {
        super(context);
        mActivity = _activity;
        createCards();
    }


    private void createCards() {
        DaysOfWeek _previousDay = DaysOfWeek.MON;
        mRootRelativeLayout = mActivity.findViewById(R.id.act_SWHours_RL_RootCV);
        for (DaysOfWeek _day : DaysOfWeek.values()) {
            CardView _cardView = new CardView(mActivity);
            RelativeLayout.LayoutParams _layoutParamsCV = new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
            );
            RelativeLayout.LayoutParams _layoutParamsCB =  newRLLayoutParams();
            RelativeLayout.LayoutParams _layoutParamsTV =  newRLLayoutParams();
            RelativeLayout.LayoutParams _layoutParamsSPS =  newRLLayoutParams();
            RelativeLayout.LayoutParams _layoutParamsSPE =  newRLLayoutParams();

            RelativeLayout _relativeLayoutInCard = new RelativeLayout(mActivity);
            _relativeLayoutInCard.setLayoutParams(_layoutParamsCV);
            _relativeLayoutInCard.setPadding(2, 4, 2, 4);

            if(_day.geteDisplayName().equals("Saturday")) {
                _layoutParamsCV.addRule(RelativeLayout.BELOW, DaysOfWeek.ALL.getCardViewId());
            }
            else if(_day.geteDisplayName().equals("Monday") || _day.geteDisplayName().equals("All")) {
                _layoutParamsCV.addRule(RelativeLayout.BELOW, R.id.act_SWHours_CB_DiffHours);
            }
            else {
                _layoutParamsCV.addRule(RelativeLayout.BELOW, _previousDay.getCardViewId());
            }

            addCV(_layoutParamsCV, _cardView, _relativeLayoutInCard, _day);
            addCB(_layoutParamsCB, _relativeLayoutInCard, _day);
            addTV(_layoutParamsTV, _relativeLayoutInCard, _day);
            addSpinnerEnd(_layoutParamsSPE, _relativeLayoutInCard, _day);
            addSpinnerStart(_layoutParamsSPS, _relativeLayoutInCard, _day);

            _previousDay = _day;
        }

        initializeMap();
    }

    private void addSpinnerStart(RelativeLayout.LayoutParams layoutParamsSPS, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        Spinner _spinnerStart = new Spinner(mActivity, Spinner.MODE_DIALOG);
        _spinnerStart.setId(day.geteSpinnerStartID());
        _spinnerStart.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleStart));
        relativeLayoutInCard.addView(_spinnerStart, layoutParamsSPS);
        layoutParamsSPS.addRule(RelativeLayout.START_OF, day.geteSpinnerEndID());
        layoutParamsSPS.addRule(RelativeLayout.CENTER_VERTICAL);
        layoutParamsSPS.setMargins(0, 0, 0, 0);
        _spinnerStart.setLayoutParams(layoutParamsSPS);
    }

    private void addSpinnerEnd(RelativeLayout.LayoutParams layoutParamsSPE, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        Spinner _spinnerEnd = new Spinner(mActivity, Spinner.MODE_DIALOG);
        _spinnerEnd.setId(day.geteSpinnerEndID());
        _spinnerEnd.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleEnd));
        relativeLayoutInCard.addView(_spinnerEnd, layoutParamsSPE);
        layoutParamsSPE.addRule(RelativeLayout.ALIGN_PARENT_END);
        layoutParamsSPE.addRule(RelativeLayout.CENTER_VERTICAL);
        _spinnerEnd.setLayoutParams(layoutParamsSPE);
    }

    private void addTV(RelativeLayout.LayoutParams layoutParamsTV, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        TextView _TVDayLabel = new TextView(mActivity);
        _TVDayLabel.setText(day.geteDisplayName().substring(0, 3));
        _TVDayLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        relativeLayoutInCard.addView(_TVDayLabel, layoutParamsTV);
        layoutParamsTV.addRule(RelativeLayout.END_OF, day.geteCheckBoxID());
        layoutParamsTV.setMargins(12, 0, 0, 6);
        layoutParamsTV.addRule(RelativeLayout.CENTER_VERTICAL);
        _TVDayLabel.setLayoutParams(layoutParamsTV);
    }

    private void addCB(RelativeLayout.LayoutParams layoutParamsCB, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        CheckBox _checkBox = new CheckBox(mActivity);
        _checkBox.setId(day.geteCheckBoxID());
        _checkBox.setText(R.string.act_SWHours_CB_FreeDay);
        relativeLayoutInCard.addView(_checkBox, layoutParamsCB);
        layoutParamsCB.addRule(RelativeLayout.ALIGN_PARENT_START);
        layoutParamsCB.setMargins(0,  0, 12, 0);
        layoutParamsCB.addRule(RelativeLayout.CENTER_VERTICAL);
        _checkBox.setLayoutParams(layoutParamsCB);
    }

    private void addCV(RelativeLayout.LayoutParams layoutParamsCV, CardView cardView, RelativeLayout relativeLayoutInCard, DaysOfWeek day) {
        layoutParamsCV.setMargins(64, 12, 64, 12);
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


    private void initializeMap() {
        int _counter = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(_counter < 6 && _counter > 0) {
                mActivity.findViewById(_day.getCardViewId()).setVisibility(View.GONE);
            }
            _counter++;
        }
    }

    public void setVisibilityOnCheck(boolean isChecked) {
        int _counter = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(_counter < 6 && _counter > 0) {
                if (isChecked) {
                    mActivity.findViewById(_day.getCardViewId()).setVisibility(View.VISIBLE);
                } else {
                    mActivity.findViewById(_day.getCardViewId()).setVisibility(View.GONE);
                }
            }
            _counter++;
        }
        if(isChecked) {
            mActivity.findViewById(DaysOfWeek.ALL.getCardViewId()).setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mActivity.findViewById(DaysOfWeek.SAT.getCardViewId()).getLayoutParams();
            params.addRule(RelativeLayout.BELOW, DaysOfWeek.FRI.getCardViewId());
            mActivity.findViewById(DaysOfWeek.SAT.getCardViewId()).setLayoutParams(params);
        }
        else {
            mActivity.findViewById(DaysOfWeek.ALL.getCardViewId()).setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mActivity.findViewById(DaysOfWeek.SAT.getCardViewId()).getLayoutParams();
            params.addRule(RelativeLayout.BELOW, DaysOfWeek.ALL.getCardViewId());
            mActivity.findViewById(DaysOfWeek.SAT.getCardViewId()).setLayoutParams(params);
        }
    }
}
