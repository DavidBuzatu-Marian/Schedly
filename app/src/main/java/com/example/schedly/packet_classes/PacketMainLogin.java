package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.schedly.CalendarActivity;
import com.example.schedly.R;
import com.example.schedly.ScheduleDurationActivity;
import com.example.schedly.SetPhoneNumberActivity;
import com.example.schedly.SetProfessionActivity;
import com.example.schedly.SetWorkingHoursActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.SPN_CANCEL;
import static com.example.schedly.MainActivity.SD_CANCEL;
import static com.example.schedly.MainActivity.SP_CANCEL;
import static com.example.schedly.MainActivity.SWH_CANCEL;

public class PacketMainLogin {
    private final String TAG = "PacketMain";
    private Activity mActivity;
    private boolean mIsMain;
    /* firestore */
    FirebaseFirestore mFirebaseFirestore;
    /* store user info */
    private String mUserPhoneNumber;
    private String mUserProfession;
    private String mUserAppointmentsDuration;
    private ProgressBar mProgressBar;
    private ConstraintLayout mRootConstraintLayout;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private String mUserDaysWithScheduleID;
    private boolean mDialogExists;


    public PacketMainLogin(Activity activity, boolean isMain) {
        mActivity = activity;
        mIsMain = isMain;
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        if (mIsMain) {
            /* we come from main activity
             * get views for progress bar
             */
            mProgressBar = mActivity.findViewById(R.id.act_main_PB);
            mRootConstraintLayout = mActivity.findViewById(R.id.act_main_CL_Root);
        }
    }

    public void getUserDetails(@NonNull final FirebaseUser currentUser) {
        DocumentReference documentReference = mFirebaseFirestore.collection("users").document(currentUser.getUid());
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot _document = task.getResult();
                    if (_document.exists()) {
                        documentExistsCode(_document, currentUser);
                    } else {
                        documentNotExists(currentUser);
                    }
                } else {
                    mUserPhoneNumber = null;
                    mUserProfession = null;
                    redirectUser(currentUser);
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void documentNotExists(FirebaseUser currentUser) {
        mUserPhoneNumber = null;
        mUserProfession = null;
        addUserToDatabase(currentUser);
    }

    private void addUserToDatabase(final FirebaseUser currentUser) {
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", null);
        userToAdd.put("profession", null);
        mFirebaseFirestore.collection("users")
                .document(currentUser.getUid())
                .set(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        addUserWorkingDays(currentUser);
                    }
                });
    }

    private void addUserWorkingDays(final FirebaseUser currentUser) {
        /* add days of the week to collection */
        Map<String, Object> daysOfTheWeek = getInitMap();
        mFirebaseFirestore.collection("workingDays")
                .document(currentUser.getUid())
                .set(daysOfTheWeek)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        redirectUser(currentUser);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failed to save Working Days");
                    }
                });
    }

    private Map<String, Object> getInitMap() {
        Map<String, Object> _data = new HashMap<>();
        _data.put("MondayStart", null);
        _data.put("MondayEnd", null);
        _data.put("TuesdayStart", null);
        _data.put("TuesdayEnd", null);
        _data.put("WednesdayStart", null);
        _data.put("WednesdayEnd", null);
        _data.put("ThursdayStart", null);
        _data.put("ThursdayEnd", null);
        _data.put("FridayStart", null);
        _data.put("FridayEnd", null);
        _data.put("SaturdayStart", null);
        _data.put("SaturdayEnd", null);
        _data.put("SundayStart", null);
        _data.put("SundayEnd", null);
        return _data;
    }

    private void documentExistsCode(DocumentSnapshot document, FirebaseUser currentUser) {
        mUserPhoneNumber = document.get("phoneNumber") != null ? document.get("phoneNumber").toString() : null;
        mUserProfession = document.get("profession") != null ? document.get("profession").toString() : null;
        mUserAppointmentsDuration = document.get("appointmentsDuration") != null ? document.get("appointmentsDuration").toString() : null;
        mUserDaysWithScheduleID = document.get("daysWithScheduleID") != null ? document.get("daysWithScheduleID").toString() : null;

        if (mUserPhoneNumber == null || mUserProfession == null) {
            redirectUser(currentUser);
        } else {
            checkWorkingDaysSetup(currentUser);
        }
    }

    private void redirectUser(final FirebaseUser localUser) {
        /* REDIRECT */
        if (mUserPhoneNumber == null || mUserProfession == null) {
            getToInitActivity(localUser);
        } else {
            checkWorkingDaysSetup(localUser);
        }
    }

    private void getToInitActivity(FirebaseUser user) {
        if (mIsMain) {
            showProgressBar(false);
        }
        if (mUserPhoneNumber == null) {
            Intent _phoneNumberIntent = new Intent(mActivity, SetPhoneNumberActivity.class);
            _phoneNumberIntent.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(_phoneNumberIntent, SPN_CANCEL);
        } else if (mUserProfession == null) {
            Intent _professionIntent = new Intent(mActivity, SetProfessionActivity.class);
            _professionIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
            _professionIntent.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(_professionIntent, SP_CANCEL);
        } else {
            Intent _workingDaysIntent = new Intent(mActivity, SetWorkingHoursActivity.class);
            _workingDaysIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
            _workingDaysIntent.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(_workingDaysIntent, SWH_CANCEL);
        }
    }

    private void checkWorkingDaysSetup(final FirebaseUser currentUser) {
        Log.d("CheckPacketMain", "CheckingWorkingHours");
        final FirebaseUser localUser = currentUser;
        mFirebaseFirestore.collection("workingDays")
                .document(currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if (document.getData() == null ) {
                            addUserWorkingDays(currentUser);
                        } else if (!document.getData().containsValue(null)) {
                            getWorkingHours(task, localUser);
                        } else {
                            getToInitActivity(currentUser);
                        }
                    }
                });
    }

    private void getWorkingHours(Task<DocumentSnapshot> task, FirebaseUser localUser) {
        Map<String, Object> _map = task.getResult().getData();
        Log.d("GettingHoursPacketMain", _map.toString());
        for (Map.Entry<String, Object> _entry : _map.entrySet()) {
            Log.d("Appointment", _entry.getKey());
            mWorkingHours.put(_entry.getKey(), _entry.getValue().toString());
        }

        getToCalendarActivity(localUser);
    }

    private void getToCalendarActivity(FirebaseUser user) {
        if (mIsMain) {
            showProgressBar(false);
        }
        if (mUserAppointmentsDuration == null) {
            Intent _scheduleDurationIntent = new Intent(mActivity, ScheduleDurationActivity.class);
            _scheduleDurationIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
            _scheduleDurationIntent.putExtra("userWorkingHours", mWorkingHours);
            _scheduleDurationIntent.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(_scheduleDurationIntent, SD_CANCEL);
        } else {
            Log.d("StartinCalPacketMain", "Start");
            Intent _calendarIntent = new Intent(mActivity, CalendarActivity.class);
            _calendarIntent.putExtra("userID", user.getUid());
            _calendarIntent.putExtra("userWorkingHours", mWorkingHours);
            _calendarIntent.putExtra("userDaysWithScheduleID", mUserDaysWithScheduleID);
            _calendarIntent.putExtra("userAppointmentDuration", mUserAppointmentsDuration);
            mActivity.startActivityForResult(_calendarIntent, CA_CANCEL);
            mActivity.finish();
        }
    }


    public void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRootConstraintLayout.setClickable(false);
            mRootConstraintLayout.setEnabled(false);
            if (!mDialogExists) {
                disableView(false);
            }
        } else {
            mProgressBar.setVisibility(View.GONE);
            mRootConstraintLayout.setClickable(true);
            mRootConstraintLayout.setEnabled(true);
            if (!mDialogExists) {
                disableView(true);
            }
        }

    }

    private void disableView(boolean value) {
        ViewGroup _viewGroup = mActivity.findViewById(R.id.act_main_CL_Root);
        loopThroughViews(_viewGroup, value);
        mActivity.findViewById(R.id.act_main_TIL_email).setEnabled(value);
        _viewGroup = mActivity.findViewById(R.id.act_main_RL_CV_Password);
        loopThroughViews(_viewGroup, value);
    }

    private void loopThroughViews(ViewGroup viewGroup, boolean value) {
        int _childrenNumber = viewGroup.getChildCount(), _counter;
        for (_counter = 0; _counter < _childrenNumber; _counter++) {
            View _childView = viewGroup.getChildAt(_counter);
            _childView.setEnabled(value);
            Log.d("Views", _childView.toString());
        }
        viewGroup.setEnabled(value);
    }

    public void setDialogViewExists(boolean value) {
        mDialogExists = value;
    }
}
