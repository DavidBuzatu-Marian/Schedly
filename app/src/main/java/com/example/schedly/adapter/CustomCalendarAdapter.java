package com.example.schedly.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.example.schedly.R;
import com.example.schedly.model.CircularTextView;
import com.example.schedly.model.CustomCalendarView;
import com.example.schedly.model.CustomEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class CustomCalendarAdapter extends ArrayAdapter<Date> {
    private LayoutInflater inflater;
    private HashMap<Long, CustomEvent> eventDays;
    private Context mContext;
    private CircularTextView mCircularTV = null;
    private Calendar mCalendar;
    private Calendar mCalendarToday;
    private CustomCalendarView mCustomCalendarView;

    public CustomCalendarAdapter(Context context, CustomCalendarView customCalendarView,
                                 ArrayList<Date> days, HashMap<Long, CustomEvent> eventDays,
                                 Calendar calendar, Calendar calendarToday) {

        super(context, R.layout.custom_calendar_day, days);
        mContext = context;
        mCustomCalendarView = customCalendarView;
        this.eventDays = eventDays;
        mCalendar = calendar;
        mCalendarToday = calendarToday;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // day in question
        Calendar calendar = Calendar.getInstance();
        Date date = getItem(position);
        calendar.setTime(date);
        final int _fday = calendar.get(Calendar.DATE);
        final int _fmonth = calendar.get(Calendar.MONTH);
        final int _fyear = calendar.get(Calendar.YEAR);

        Calendar _curMonth = mCalendar;
        // inflate item if it does not exist yet
        if (view == null)
            view = inflater.inflate(R.layout.custom_calendar_day, parent, false);
        // clear styling
        final CircularTextView _fcircularTextView = view.findViewById(R.id.textView);
        _fcircularTextView.setTypeface(null, Typeface.NORMAL);
        _fcircularTextView.setTextColor(Color.BLACK);
        _fcircularTextView.setCalendar(calendar);

        if (_fmonth != _curMonth.get(Calendar.MONTH) || _fyear != _curMonth.get(Calendar.YEAR)) {
            // if this day is outside current month, hide it
            view.setVisibility(View.GONE);
        } else if (isToday(_fday, _fmonth, _fyear) && !mCustomCalendarView.isMarkedDate()) {
            // it is today, mark it with a different text color on init.
            if (mCircularTV == null || mCircularTV.getCalendar() == mCalendarToday) {
                markSelectedDate(_fcircularTextView);
                mCircularTV = _fcircularTextView;
            }
        } else {
            // check if day is weekend or weekday
            if(!isToday(_fday, _fmonth, _fyear)) {
                markDateByDay(calendar, _fcircularTextView);
            } else {
                /* mark current day always with red color text */
                _fcircularTextView.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
            }
        }

        // set text
        _fcircularTextView.setText(String.valueOf(calendar.get(Calendar.DATE)));
        _fcircularTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCircularTV != null) {
                    mCircularTV.setBackgroundResource(R.drawable.round_textview);
                    markDateByDay(mCircularTV.getCalendar(), mCircularTV);
                    if(mCircularTV.getCalendar().equals(mCalendarToday)) {
                        mCircularTV.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
                    }
                }
                markSelectedDate(_fcircularTextView);
                mCircularTV = _fcircularTextView;
                mCustomCalendarView.setMarkedDate(_fcircularTextView.getCalendar());
                mCustomCalendarView.performClick();

            }
        });

        /* mark it with the event */
        if(eventDays != null && eventDays.containsKey(calendar.getTimeInMillis()) && mCircularTV != null && mCircularTV.getCalendar() != calendar) {
            _fcircularTextView.setBackgroundResource(eventDays.get(calendar.getTimeInMillis()).getDayStatus());
        }

        /* mark the day on back or next */
        if (mCustomCalendarView.isMarkedDate()) {
            Calendar _markedDate = mCustomCalendarView.getMarkedDay();
            if (_fday == _markedDate.get(Calendar.DATE) && _fmonth == _markedDate.get(Calendar.MONTH) && _fyear == _markedDate.get(Calendar.YEAR)) {
                markSelectedDate(_fcircularTextView);
                mCircularTV = _fcircularTextView;
            }
        }

        return view;
    }

    private boolean isToday(int day, int month, int year) {
        return (day == mCalendarToday.get(Calendar.DATE) && month == mCalendarToday.get(Calendar.MONTH) && year == mCalendarToday.get(Calendar.YEAR));
    }


    private void markSelectedDate(CircularTextView circularTextView) {
        circularTextView.setBackgroundResource(R.drawable.round_view);
        circularTextView.setGravity(Gravity.CENTER);
        circularTextView.setTextColor(Color.WHITE);

    }

    private void markDateByDay(Calendar calendar, CircularTextView circularTextView) {
        int _dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (_dayOfWeek == Calendar.SATURDAY || _dayOfWeek == Calendar.SUNDAY) {
            circularTextView.setTextColor(Color.parseColor("#489e4b"));
        } else {
            circularTextView.setTextColor(Color.parseColor("#2d87e2"));
        }
        if(eventDays != null && eventDays.containsKey(calendar.getTimeInMillis())) {
            circularTextView.setBackgroundResource(eventDays.get(calendar.getTimeInMillis()).getDayStatus());
        }
    }
}
