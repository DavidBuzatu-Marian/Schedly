package com.davidbuzatu.schedly.model;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.adapter.CustomCalendarAdapter;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;
import org.threeten.bp.YearMonth;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.TextStyle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class CustomCalendarView extends LinearLayout {
    // calendar components
    private TextView mTXTDisplayDate;
    private GridView mGridView;
    private LocalDate mDateNow;
    private final int DAYS_COUNT = 37;
    private long mCurrentDateInMillis;
    private boolean mFirstInstance = true;
    private Calendar mCalendarToday;
    private Calendar mMarkedDay = null;
    private HashMap<Long, CustomEvent> mEvents;

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControl(context, attrs);
    }

    private void assignUiElements() {
        mTXTDisplayDate = findViewById(R.id.date_display_date);
        mGridView = findViewById(R.id.calendar_grid);
        mDateNow = LocalDate.now().atStartOfDay(ZoneOffset.systemDefault()).toLocalDate();
        mCurrentDateInMillis = mDateNow.atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        mCalendarToday = Calendar.getInstance();
        mCalendarToday.setTimeInMillis(mCurrentDateInMillis);
    }
    private void initControl(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_calendar_view, this);
        assignUiElements();
    }

    public void updateCalendar(HashMap<Long, CustomEvent> events) {
        mEvents = events;
        ArrayList<Date> _cells = new ArrayList<>();
        long _time = YearMonth.from(mDateNow).atDay(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

        Calendar _calendar = getCalendarAfterRemovingPrevDates(_time);
        fillCells(_cells ,_calendar);
        _calendar.setTimeInMillis(getTimeForInstance(_time));
        mFirstInstance = false;

        mGridView.setAdapter(new CustomCalendarAdapter(getContext(), this, _cells, events, _calendar, mCalendarToday));
        displayDateInTXT();
    }

    private void displayDateInTXT() {
        LocalDate _localeDate = LocalDate.of(mDateNow.getYear(), mDateNow.getMonthValue(), mDateNow.getDayOfMonth());
        Month _month = _localeDate.getMonth();
        String _date = _month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + mDateNow.getYear();
        mTXTDisplayDate.setText(_date);
    }

    private Calendar getCalendarAfterRemovingPrevDates(long time) {
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(time);
        int _monthBeginningCell = _calendar.get(Calendar.DAY_OF_WEEK) - 1;
        // move calendar backwards to the beginning of the week
        _calendar.add(Calendar.DAY_OF_MONTH, -_monthBeginningCell);
        return _calendar;
    }

    private void fillCells(ArrayList<Date> cells, Calendar calendar) {
        while (cells.size() < DAYS_COUNT) {
            cells.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private long getTimeForInstance(long time) {
        return mFirstInstance ? mCurrentDateInMillis : time;
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
