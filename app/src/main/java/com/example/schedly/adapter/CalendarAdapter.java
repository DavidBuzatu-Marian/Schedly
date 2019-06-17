package com.example.schedly.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.R;
import com.example.schedly.model.Appointment;

import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarScheduleViewHolder> {


    private ArrayList<Appointment> mDataSet;
    private AppCompatActivity mActivity;;

    public CalendarAdapter(AppCompatActivity activity, ArrayList<Appointment> dataset) {
        mActivity = activity;
        mDataSet = dataset;
    }

    @NonNull
    @Override
    public CalendarScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout viewgroup = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.appointment_item, parent, false);

        CalendarScheduleViewHolder vh = new CalendarScheduleViewHolder(viewgroup);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarScheduleViewHolder holder, int position) {
        Appointment appointment = mDataSet.get(position);
        holder.updateDay(appointment);
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public class CalendarScheduleViewHolder extends RecyclerView.ViewHolder {

        public CalendarScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void updateDay(Appointment appointment) {

        }
    }

}
