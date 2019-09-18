package com.example.schedly.model;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.schedly.R;
import com.example.schedly.adapter.CustomCalendarAdapter;

import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;
import org.threeten.bp.ZoneOffset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class CustomCalendarView extends LinearLayout {
    // calendar components
    private TextView mTXTDisplayDate;
    private GridView mGridView;
    private LocalDate mDateNow;
    private final int DAYS_COUNT = 37;
    private long mCurrentDateInMillis;
    private boolean mFirstInstance;
    private Calendar mCalendarToday;
    private Calendar mMarkedDay = null;
    private HashMap<Long, CustomEvent> mEvents;
    private ImageView mBUTPrev, mBUTNext;

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControl(context, attrs);
    }

    private void assignUiElements() {
        // layout is inflated, assign local variables to components
        mTXTDisplayDate = findViewById(R.id.date_display_date);
        mGridView = findViewById(R.id.calendar_grid);
        mDateNow = LocalDate.now().atStartOfDay(ZoneOffset.systemDefault()).toLocalDate();
        mCurrentDateInMillis = mDateNow.atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        mFirstInstance = true;
        mBUTPrev = findViewById(R.id.calendar_prev_button);
        mBUTNext = findViewById(R.id.calendar_next_button);
    }

    /**
     * Load control xml layout
     */
    private void initControl(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_calendar_view, this);
        assignUiElements();
    }

    public void updateCalendar(HashMap<Long, CustomEvent> events) {
        mEvents = events;
        ArrayList<Date> _cells = new ArrayList<>();
        long _time = YearMonth.from(mDateNow).atDay(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(_time);

        int _monthBeginningCell = _calendar.get(Calendar.DAY_OF_WEEK) - 1;

        // move calendar backwards to the beginning of the week
        _calendar.add(Calendar.DAY_OF_MONTH, -_monthBeginningCell);
        // fill cells
        while (_cells.size() < DAYS_COUNT) {
            _cells.add(_calendar.getTime());
            _calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        /* update grid. if first update, mark date.
         * else take the first day of the month
         */
        if(mFirstInstance) {
            mCalendarToday = Calendar.getInstance();
            mCalendarToday.setTimeInMillis(mCurrentDateInMillis);
            _calendar.setTimeInMillis(mCurrentDateInMillis);
        } else {
            _calendar.setTimeInMillis(_time);
        }

        mGridView.setAdapter(new CustomCalendarAdapter(getContext(), this, _cells, events, _calendar, mCalendarToday));
        mGridView.setOnTouchListener(new CustomOnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                mBUTNext.performClick();
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                mBUTPrev.performClick();
            }
        });
        mFirstInstance = false;
        String _date = mDateNow.getMonth().toString() + " " + mDateNow.getYear();
        mTXTDisplayDate.setText(_date);
    }

    public void setMarkedDate(Calendar markedDate) {
        mMarkedDay = markedDate;
    }
    public Calendar getMarkedDay() {
        return mMarkedDay;
    }
    public boolean isMarkedDate() {
        return !(mMarkedDay == null);
    }
    public LocalDate getDate() {
        return mDateNow;
    }

    public void setDate(LocalDate date) {
        mDateNow = date;
    }
//    public void setOnClickListener(OnClickListener listener) {
//        mLinearLayout.setOnClickListener(listener);
//    }
}
