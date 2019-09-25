package com.example.schedly.adapter;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
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
    private final String mUserID;

    private ArrayList<Appointment> mDataSet;
    private AppCompatActivity mActivity;
    private Long mDateInMillis;

    public CalendarAdapter(AppCompatActivity activity, ArrayList<Appointment> dataset, String userID) {
        mActivity = activity;
        mDataSet = dataset;
        mUserID = userID;
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
        int layout_id;

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
        private PopupWindow mPopWindow;
        private String mCompleteDate;


        public CalendarScheduleViewHolder(@NonNull View itemView, @NonNull final ViewGroup parent) {
            super(itemView);

            mTextViewHour = itemView.findViewById(R.id.appointment_item_TV_Hour);
            mTextViewName = itemView.findViewById(R.id.appointment_item_TV_Name);
            mTextViewPhoneNumber = itemView.findViewById(R.id.appointment_item_TV_PhoneNumber);
            mImageViewEdit = itemView.findViewById(R.id.appointment_item_IV_AppointmentOptions);

            new BubbleShowCaseBuilder(mActivity)
                    .title("Edit appointment") //Any title for the bubble view
                    .backgroundColor(R.color.colorPrimary)
                    .description(mActivity.getString(R.string.helpEditExplained))
                    .targetView(mImageViewEdit)//View to point out
                    .showOnce("FirstTimerEdit")
                    .show(); //Display the ShowCase

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
                            .setTitle(mActivity.getString(R.string.edit_appointment_cancel))
                            .setMessage(mActivity.getString(R.string.edit_appointment_cancel_dialog))
                            .setIcon(R.drawable.ic_baseline_cancel_24px)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String _phoneNumber = mTextViewPhoneNumber.getText().toString();
                                    String _hour = mTextViewHour.getText().toString();

                                    Map<String, Object> _deleteAppointment = new HashMap<>();
                                    _deleteAppointment.put(mDateInMillis + "." + mTextViewHour.getText().toString(), FieldValue.delete());
                                    FirebaseFirestore.getInstance().collection("scheduledHours")
                                            .document(mUserID)
                                            .update(_deleteAppointment);

                                    mPopWindow.dismiss();

//                                    sendMessage(_phoneNumber, _hour);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }

        private void sendMessage(String phoneNumber, String hour) {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null,
                    mActivity.getString(R.string.edit_appointment_cancel_message_start)
                            + mCompleteDate
                            + mActivity.getString(R.string.add_appointment_manual_success_at)
                            + hour 
                            + mActivity.getString(R.string.edit_appointment_message_end),
                    null, null);
            Log.d("MESSAGE_ON_CANCEL_app", "CANCELED");
        }

        private void setInformationInPopup(View inflatedView) {
            TextView _textViewName = inflatedView.findViewById(R.id.popup_appointment_TV_Name);
            TextView _textViewPhoneNumber = inflatedView.findViewById(R.id.popup_appointment_TV_PhoneNumber);
            TextView _textViewAppointmentInfo = inflatedView.findViewById(R.id.popup_appointment_TV_AppointmentInfo);
            String _textForInfoCard = mActivity.getString(R.string.edit_appointment_info_app_start) + mTextViewHour.getText();
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

        private void updateDay(Appointment appointment) {
            String _name = appointment.getmName();
            mTextViewHour.setText(appointment.getmHour());
            mCompleteDate = appointment.getmDate();
            mDateInMillis = appointment.getmDateInMillis();
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
