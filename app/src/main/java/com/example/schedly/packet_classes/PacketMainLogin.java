package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.schedly.MainActivity;
import com.example.schedly.R;
import com.example.schedly.ScheduleDurationActivity;
import com.example.schedly.SetPhoneNumberActivity;
import com.example.schedly.SetProfessionActivity;
import com.example.schedly.SetWorkingHoursActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
    private Activity mActivity;
    private final String TAG = "RES";
    private boolean mIsMain;
    /* firestore */
    FirebaseFirestore mFirebaseFirestore;
    /* store user info */
    private String mUserWorkingHoursID;
    private String mUserPhoneNumber;
    private String mUserProfession;
    private String mUserAppointmentsDuration;
    private ProgressBar mProgressBar;
    private ConstraintLayout mRootConstraintLayout;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private String mUserDaysWithScheduleID;
    private boolean mDialogExists;


    public PacketMainLogin(Activity activity,  boolean isMain) {
        mActivity = activity;
        mIsMain = isMain;
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        if(mIsMain) {
            /* we come from main activity
             * get views for progress bar
             */
            mProgressBar = mActivity.findViewById(R.id.act_main_PB);
            mRootConstraintLayout= mActivity.findViewById(R.id.act_main_CL_Root);
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
                        Log.d(TAG, "Success");
                        mUserPhoneNumber = _document.get("phoneNumber") != null ? _document.get("phoneNumber").toString() : null;
                        mUserProfession = _document.get("profession") != null ? _document.get("profession").toString() : null;
                        mUserWorkingHoursID = _document.get("workingDaysID") != null ? _document.get("workingDaysID").toString() : null;
                        mUserAppointmentsDuration = _document.get("appointmentsDuration") != null ? _document.get("appointmentsDuration").toString() : null;
                        mUserDaysWithScheduleID = _document.get("daysWithScheduleID") != null ? _document.get("daysWithScheduleID").toString() : null;

                        if(mUserPhoneNumber == null || mUserProfession == null) {
                            redirectUser(currentUser);
                        }
                        else {
                            if (mUserWorkingHoursID == null) {
                                addUserWorkingDaysID(currentUser);
                            } else {
                                checkWorkingDaysSetup(currentUser);
                            }
                        }
                    } else {
                        mUserPhoneNumber = null;
                        mUserProfession = null;
                        addUserToDatabase(currentUser);
                        addUserWorkingDaysID(currentUser);
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

    private void getWorkingHours(Task<DocumentSnapshot> task, FirebaseUser localUser) {
        Map<String, Object> _map = task.getResult().getData();
        Log.d("GettingHoursPacketMain", _map.toString());
        for (Map.Entry<String, Object> _entry : _map.entrySet()) {
            Log.d("Appointment", _entry.getKey());
            mWorkingHours.put(_entry.getKey(), _entry.getValue().toString());
        }

        getToCalendarActivity(localUser);
    }

    private void checkWorkingDaysSetup(FirebaseUser currentUser) {
        Log.d("CheckPacketMain", "CheckingWorkingHours");
        final FirebaseUser localUser = currentUser;
        mFirebaseFirestore.collection("workingDays")
                .document(mUserWorkingHoursID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if(document.getData().containsValue(null)) {
                            getToInitActivity(localUser);
                        }
                        else {
                            getWorkingHours(task, localUser);
                        }
                    }
                });
    }

    private void addUserToDatabase(FirebaseUser user) {
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", null);
        userToAdd.put("profession", null);
        mFirebaseFirestore.collection("users")
                .document(user.getUid())
                .set(userToAdd);
    }

    private void addUserWorkingDaysID(final FirebaseUser currentUser) {
        /* add days of the week to collection */
        Map<String, Object> daysOfTheWeek = new HashMap<>();
        daysOfTheWeek.put("MondayStart", null);
        daysOfTheWeek.put("MondayEnd", null);
        daysOfTheWeek.put("TuesdayStart", null);
        daysOfTheWeek.put("TuesdayEnd", null);
        daysOfTheWeek.put("WednesdayStart", null);
        daysOfTheWeek.put("WednesdayEnd", null);
        daysOfTheWeek.put("ThursdayStart", null);
        daysOfTheWeek.put("ThursdayEnd", null);
        daysOfTheWeek.put("FridayStart", null);
        daysOfTheWeek.put("FridayEnd", null);
        daysOfTheWeek.put("SaturdayStart", null);
        daysOfTheWeek.put("SaturdayEnd", null);
        daysOfTheWeek.put("SundayStart", null);
        daysOfTheWeek.put("SundayEnd", null);
        mFirebaseFirestore.collection("workingDays")
                .add(daysOfTheWeek)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
                        Map<String, Object> userToAdd = new HashMap<>();
                        Log.d(TAG, "DocumentSnapshot written:" + documentReference.getId());
                        mUserWorkingHoursID = documentReference.getId();
                        userToAdd.put("workingDaysID", mUserWorkingHoursID);
                        mFireStore.collection("users")
                                .document(currentUser.getUid())
                                .update(userToAdd)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        redirectUser(currentUser);
                                        Log.d(TAG, "DocumentSnapshot successfully written!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void redirectUser(final FirebaseUser localUser) {
        /* REDIRECT */
        if(mUserPhoneNumber == null || mUserProfession == null) {
            getToInitActivity(localUser);
        }
        else {
            checkWorkingDaysSetup(localUser);
        }
    }


    private void getToInitActivity(FirebaseUser user) {
        if(mIsMain) {
            showProgressBar(false);
        }
        if(mUserPhoneNumber == null) {
            Intent firstStep = new Intent(mActivity, SetPhoneNumberActivity.class);
            firstStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(firstStep, SPN_CANCEL);
        }
        else if(mUserProfession == null) {
            Intent secondStep = new Intent(mActivity, SetProfessionActivity.class);
            secondStep.putExtra("userPhoneNumber", mUserPhoneNumber);
            secondStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(secondStep, SP_CANCEL);
        } else {
            Intent thirdStep = new Intent(mActivity, SetWorkingHoursActivity.class);
            thirdStep.putExtra("userPhoneNumber", mUserPhoneNumber);
            thirdStep.putExtra("userWorkingHoursID", mUserWorkingHoursID);
            thirdStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(thirdStep, SWH_CANCEL);
        }
    }

    private void getToCalendarActivity(FirebaseUser user) {
        if(mIsMain) {
            showProgressBar(false);
        }
        if(mUserAppointmentsDuration == null) {
            Intent ScheduleDuration = new Intent(mActivity, ScheduleDurationActivity.class);
            ScheduleDuration.putExtra("userPhoneNumber", mUserPhoneNumber);
            ScheduleDuration.putExtra("userWorkingHoursID", mUserWorkingHoursID);
            ScheduleDuration.putExtra("userWorkingHours", mWorkingHours);
            ScheduleDuration.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(ScheduleDuration, SD_CANCEL);
        }
        else {
            Log.d("StartinCalPacketMain", "Start");
            Intent CalendarActivity = new Intent(mActivity, com.example.schedly.CalendarActivity.class);
            CalendarActivity.putExtra("userID", user.getUid());
            CalendarActivity.putExtra("userWorkingHours", mWorkingHours);
            CalendarActivity.putExtra("userDaysWithScheduleID", mUserDaysWithScheduleID);
            CalendarActivity.putExtra("userAppointmentDuration", mUserAppointmentsDuration);
            CalendarActivity.putExtra("userWorkingHoursID", mUserWorkingHoursID);
            mActivity.startActivityForResult(CalendarActivity, CA_CANCEL);
        }
    }


    public void showProgressBar(boolean show) {
        if(show) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRootConstraintLayout.setClickable(false);
            mRootConstraintLayout.setEnabled(false);
            if(!mDialogExists) {
                disableView(false);
            }
        }
        else {
            mProgressBar.setVisibility(View.GONE);
            mRootConstraintLayout.setClickable(true);
            mRootConstraintLayout.setEnabled(true);
            if(!mDialogExists) {
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
        for(_counter = 0; _counter < _childrenNumber; _counter++) {
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
