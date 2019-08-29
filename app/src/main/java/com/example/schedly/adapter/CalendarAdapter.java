package com.example.schedly.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.CalendarActivity;
import com.example.schedly.R;
import com.example.schedly.model.Appointment;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarScheduleViewHolder> {

    private static final int TOP_POSITION = 0;
    private static final int BOTTOM_POSITION = 1;
    private static final int REGULAR_POSITION = 2;

    private ArrayList<Appointment> mDataSet;
    private AppCompatActivity mActivity;

    public CalendarAdapter(AppCompatActivity activity, ArrayList<Appointment> dataset) {
        mActivity = activity;
        mDataSet = dataset;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            // we are at the top element, first in the list
            return TOP_POSITION;
        }

        if (position == getItemCount() - 1) {
            // we are at the last element
            return BOTTOM_POSITION;
        }

        return REGULAR_POSITION;
    }

    @NonNull
    @Override
    public CalendarScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout_id = 0;

        switch (viewType) {
            case TOP_POSITION:
                layout_id = R.layout.appointment_item_top;
                break;
            case BOTTOM_POSITION:
                layout_id = R.layout.appointment_item_bottom;
                break;
            case REGULAR_POSITION:
            default:
                layout_id = R.layout.appointment_item;
        }


        RelativeLayout _viewGroup = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(layout_id, parent, false);

        CalendarScheduleViewHolder _vh = new CalendarScheduleViewHolder(_viewGroup, parent);
        return _vh;
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarScheduleViewHolder holder, int position) {
        Appointment _appointment = mDataSet.get(position);
        holder.updateDay(_appointment);
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
        private final String mUnknownName = "Unknown";
        private String mUDWScheduleID;
        private String mCDayID;
        private PopupWindow mPopWindow;


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
            final View _inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_popup_appointment, null, false);

            // get device size
            Display _display = parent.getDisplay();
            final Point _size = new Point();
            _display.getSize(_size);

            // set height depends on the device size
            if (_size.y < 1350) {
                mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y, true);
            } else if (_size.y > 1350 && _size.y < 1900) {
                mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y * 3 / 4, true);
            } else {
                mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y / 2, true);
            }
            // set a background drawable with rounders corners
            mPopWindow.setBackgroundDrawable(parent.getResources().getDrawable(R.drawable.bkg_appointment_options));
            // make it focusable to show the keyboard to enter in `EditText`
            mPopWindow.setFocusable(true);
            // make it outside touchable to dismiss the popup window
            mPopWindow.setOutsideTouchable(true);
            mPopWindow.setAnimationStyle(R.style.PopupAnimation);

            // show the popup at bottom of the screen and set some margin at bottom ie,
            mPopWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);


            setInformationInPopup(_inflatedView);
            setPopUpButtonsListeners(_inflatedView);

            ImageView _closeImg = _inflatedView.findViewById(R.id.popup_edit_IV_Close);
            _closeImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPopWindow.dismiss();
                }
            });

        }

        private void setPopUpButtonsListeners(final View inflatedView) {
            Button _buttonMessage = inflatedView.findViewById(R.id.popup_appointment_BUT_Message);
            Button _buttonCall = inflatedView.findViewById(R.id.popup_appointment_BUT_Call);
            Button _buttonAddToContacts = inflatedView.findViewById(R.id.popup_appointment_BUT_AddToContacts);
            Button _buttonCancelAppointment = inflatedView.findViewById(R.id.popup_appointment_BUT_CancelAppointment);

            _buttonMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent _smsIntent = new Intent(Intent.ACTION_SENDTO);
                    _smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    _smsIntent.setData(Uri.parse("sms:" + mTextViewPhoneNumber.getText().toString()));
//                    smsIntent.putExtra("sms_body","Body of Message");
                    mActivity.startActivity(_smsIntent);
                }
            });
            _buttonCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent _callIntent = new Intent(Intent.ACTION_DIAL);
                    _callIntent.setData(Uri.parse("tel:" + mTextViewPhoneNumber.getText().toString()));

                    mActivity.startActivity(_callIntent);
                }
            });
            if (!mTextViewName.getText().toString().equals("")) {
                _buttonAddToContacts.setVisibility(View.GONE);
            } else {
                _buttonAddToContacts.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent _addToContactsIntent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
                        _addToContactsIntent.setData(Uri.parse("tel:" + mTextViewPhoneNumber.getText().toString()));

                        mActivity.startActivity(_addToContactsIntent);
                    }
                });
            }
            _buttonCancelAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(inflatedView.getContext())
                            .setTitle("Cancel appointment")
                            .setMessage("Do you really want to cancel this appointment? A message will be sent automatically to the client")
                            .setIcon(R.drawable.ic_baseline_cancel_24px)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Map<String, Object> _deleteAppointment = new HashMap<>();
                                    _deleteAppointment.put(mTextViewHour.getText().toString(), FieldValue.delete());
                                    FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
                                    _firebaseFirestore.collection("daysWithSchedule")
                                            .document(mUDWScheduleID)
                                            .collection("scheduledHours")
                                            .document(mCDayID)
                                            .update(_deleteAppointment);
                                    mPopWindow.dismiss();
                                    int _counter = ((CalendarActivity) mActivity).getCounter() - 1;
                                    mDataSet.remove(getAdapterPosition());
                                    ((CalendarActivity) mActivity).setCounter(_counter);
                                    notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }

        private void setInformationInPopup(View inflatedView) {
            TextView _textViewName = inflatedView.findViewById(R.id.popup_appointment_TV_Name);
            TextView _textViewPhoneNumber = inflatedView.findViewById(R.id.popup_appointment_TV_PhoneNumber);
            TextView _textViewAppointmentInfo = inflatedView.findViewById(R.id.popup_appointment_TV_AppointmentInfo);
            String _textForInfoCard = "Appointment starts at: " + mTextViewHour.getText();
            //LinearLayout _linearLayoutAddToContacts = inflatedView.findViewById(R.id.popup_appointment_LL_AddToContacts);

            if (mTextViewName.getText().toString().equals("")) {
                _textViewName.setText(mUnknownName);
                _textViewPhoneNumber.setText(mTextViewPhoneNumber.getText().toString());
            } else {
                _textViewName.setText(mTextViewName.getText().toString());
                _textViewPhoneNumber.setText(mTextViewPhoneNumber.getText().toString());
            }

            _textViewAppointmentInfo.setText(_textForInfoCard);
        }

        public void updateDay(Appointment appointment) {
            mUDWScheduleID = appointment.getmUserDaysWithScheduleID();
            mCDayID = appointment.getmCurrentDayID();
            String _name = appointment.getmName();
            mTextViewHour.setText(appointment.getmHour());
            Log.d("DETECT", _name + ": " + appointment.getmPhoneNumber());
            if (_name != null) {
                mTextViewName.setText(_name);
                mTextViewPhoneNumber.setVisibility(View.GONE);
                mTextViewPhoneNumber.setText(appointment.getmPhoneNumber());
            } else {
                mTextViewName.setText("");
//                mTextViewName.setVisibility(View.GONE);
                mTextViewPhoneNumber.setVisibility(View.VISIBLE);
                mTextViewPhoneNumber.setText(appointment.getmPhoneNumber());
            }
        }
    }

}
