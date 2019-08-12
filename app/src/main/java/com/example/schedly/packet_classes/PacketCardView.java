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

public class PacketCardView extends CardView {

    private Activity mActivity;

    public PacketCardView(Context context, Activity _activity) {
        super(context);
        mActivity = _activity;
        createCards();
    }


    private void createCards() {
        /* get each day from enum and make the card
         *  with the necessary elements
         */
        DaysOfWeek _previousDay = DaysOfWeek.MON;
        RelativeLayout _rootRelativeLayout = mActivity.findViewById(R.id.act_SWHours_RL_RootCV);

        for (DaysOfWeek _day : DaysOfWeek.values()) {
            Log.d("Stop", _day.geteDisplayName());
            CardView _cardview = new CardView(mActivity);
            RelativeLayout.LayoutParams _layoutParamsCV = new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
            );
            /* for Checkbox, Textviews and spinners inside CV */
            RelativeLayout.LayoutParams _layoutParamsCB =  new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );
            RelativeLayout.LayoutParams _layoutParamsTV =  new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );
            RelativeLayout.LayoutParams _layoutParamsSPS =  new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );
            RelativeLayout.LayoutParams _layoutParamsSPE =  new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );

            RelativeLayout _relativeLayoutInCard = new RelativeLayout(mActivity);
            _relativeLayoutInCard.setLayoutParams(_layoutParamsCV);
            _relativeLayoutInCard.setPadding(2, 4, 2, 4);

            if(_day.geteDisplayName().equals("Saturday")) {
                Log.d("Stop", DaysOfWeek.ALL.getCardViewId() + "; ASTA");
                _layoutParamsCV.addRule(RelativeLayout.BELOW, DaysOfWeek.ALL.getCardViewId());
            }
            else if(_day.geteDisplayName().equals("Monday") || _day.geteDisplayName().equals("All")) {
                Log.d("Stop", "It gets here: " + _day.getCardViewId());
                _layoutParamsCV.addRule(RelativeLayout.BELOW, R.id.act_SWHours_CB_DiffHours);
            }
            else {
                _layoutParamsCV.addRule(RelativeLayout.BELOW, _previousDay.getCardViewId());
            }

            _layoutParamsCV.setMargins(64, 9, 64, 9);
            _cardview.setRadius(32);
            _cardview.setCardElevation(6);
            _cardview.setId(_day.getCardViewId());
            _cardview.setLayoutParams(_layoutParamsCV);

            _rootRelativeLayout.addView(_cardview);
            _cardview.addView(_relativeLayoutInCard);


            CheckBox _checkBox = new CheckBox(mActivity);
            _checkBox.setId(_day.geteCheckBoxID());
            _checkBox.setText(R.string.act_SWHours_CB_FreeDay);
            _relativeLayoutInCard.addView(_checkBox, _layoutParamsCB);
            _layoutParamsCB.addRule(RelativeLayout.ALIGN_PARENT_START);
            _layoutParamsCB.setMargins(0,  0, 12, 0);
            _layoutParamsCB.addRule(RelativeLayout.CENTER_VERTICAL);
            _checkBox.setLayoutParams(_layoutParamsCB);

            TextView _TVDayLabel = new TextView(mActivity);
            _TVDayLabel.setText(_day.geteDisplayName().substring(0, 3));
            _TVDayLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            _relativeLayoutInCard.addView(_TVDayLabel, _layoutParamsTV);
            _layoutParamsTV.addRule(RelativeLayout.END_OF, _day.geteCheckBoxID());
            _layoutParamsTV.setMargins(12, 0, 0, 6);
            _layoutParamsTV.addRule(RelativeLayout.CENTER_VERTICAL);
            _TVDayLabel.setLayoutParams(_layoutParamsTV);

            Spinner _spinnerEnd = new Spinner(mActivity, Spinner.MODE_DIALOG);
            _spinnerEnd.setId(_day.geteSpinnerEndID());
            _spinnerEnd.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleEnd));
            _relativeLayoutInCard.addView(_spinnerEnd, _layoutParamsSPS);
            _layoutParamsSPS.addRule(RelativeLayout.ALIGN_PARENT_END);
            _layoutParamsSPS.addRule(RelativeLayout.CENTER_VERTICAL);
            _spinnerEnd.setLayoutParams(_layoutParamsSPS);

            Spinner _spinnerStart = new Spinner(mActivity, Spinner.MODE_DIALOG);
            _spinnerStart.setId(_day.geteSpinnerStartID());
            _spinnerStart.setPrompt(getContext().getResources().getText(R.string.act_SWHours_Spinner_TitleStart));
            _relativeLayoutInCard.addView(_spinnerStart, _layoutParamsSPE);
            _layoutParamsSPE.addRule(RelativeLayout.START_OF, _day.geteSpinnerEndID());
            _layoutParamsSPE.addRule(RelativeLayout.CENTER_VERTICAL);
            _layoutParamsSPE.setMargins(0, 0, 0, 0);
            _spinnerStart.setLayoutParams(_layoutParamsSPE);


            Log.d("FAILED", _cardview.toString());
            _previousDay = _day;
        }

        initializeMap();
    }


    private void initializeMap() {
        int _counter = 0;
//        mAnimationLR = AnimationUtils.loadAnimation(activity, R.anim.cardview_transition_lr);
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(_counter < 6 && _counter > 0) {
                Log.d("FAILED", _day.geteDisplayName() + "; " + _day.getCardViewId());
                mActivity.findViewById(_day.getCardViewId()).setVisibility(View.GONE);
            }
//            mAnimationLR.setStartOffset(_counter * 150);
//            mLinearLayoutHashMap.get(mDaysOfTheWeek[_counter]).setAnimation(mAnimationLR);
            _counter++;
        }
//        this.activity.findViewById(R.id.act_SWHours_CV_Sunday).setAnimation(mAnimationLR);
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
