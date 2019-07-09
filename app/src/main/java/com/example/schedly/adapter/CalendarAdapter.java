package com.example.schedly.adapter;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.R;
import com.example.schedly.model.Appointment;

import java.util.ArrayList;

import io.opencensus.resource.Resource;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarScheduleViewHolder> {


    private ArrayList<Appointment> mDataSet;
    private AppCompatActivity mActivity;

    public CalendarAdapter(AppCompatActivity activity, ArrayList<Appointment> dataset) {
        mActivity = activity;
        mDataSet = dataset;
    }

    @NonNull
    @Override
    public CalendarScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout viewgroup = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.appointment_item, parent, false);

        CalendarScheduleViewHolder vh = new CalendarScheduleViewHolder(viewgroup, parent);
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

        private TextView mTextViewHour;
        private TextView mTextViewName;
        private TextView mTextViewPhoneNumber;
        private ImageView mImageViewEdit;

        public CalendarScheduleViewHolder(@NonNull View itemView, @NonNull final ViewGroup parent) {
            super(itemView);

            mTextViewHour = itemView.findViewById(R.id.appointment_item_TV_Hour);
            mTextViewName = itemView.findViewById(R.id.appointment_item_TV_Name);
            mTextViewPhoneNumber = itemView.findViewById(R.id.appointment_item_TV_PhoneNumber);
            mImageViewEdit = itemView.findViewById(R.id.appointment_item_IV_AppointmentOptions);

            mImageViewEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOptionsPopup(v, parent);
                }
            });

        }

        private void showOptionsPopup(View v, @NonNull ViewGroup parent) {
            // inflate the custom popup layout
            Log.d("POP", "It should trigger");
            final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_popup_appointment, null,false);

            // get device size
            Display display = parent.getDisplay();
            final Point size = new Point();
            display.getSize(size);

            PopupWindow popWindow;
            // set height depends on the device size
            popWindow = new PopupWindow(inflatedView, size.x - 50,size.y - 400, true );
//            // set a background drawable with rounders corners
            popWindow.setBackgroundDrawable(parent.getResources().getDrawable(R.drawable.bkg_appointment_options));
            // make it focusable to show the keyboard to enter in `EditText`
            popWindow.setFocusable(true);
            // make it outside touchable to dismiss the popup window
            popWindow.setOutsideTouchable(true);

            // show the popup at bottom of the screen and set some margin at bottom ie,
            popWindow.showAtLocation(v, Gravity.BOTTOM, 0,100);
        }

        public void updateDay(Appointment appointment) {
            String _name = appointment.getmName();
            mTextViewHour.setText(appointment.getmHour());
            if(_name != null) {
                mTextViewName.setText(_name);
                mTextViewPhoneNumber.setVisibility(View.GONE);
            }
            else {
                mTextViewPhoneNumber.setText(appointment.getmPhoneNumber());
            }
        }
    }

}
