package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.schedly.ScheduleDurationActivity;
import com.example.schedly.SetPhoneNumberActivity;
import com.example.schedly.SetProffesionActivity;
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
    private FirebaseAuth mAuth;
    private final String TAG = "RES";
    /* firestore */
    FirebaseFirestore mFirebaseFirestore;
    /* store user info */
    private String userWorkingHoursID;
    private String userPhoneNumber;
    private String userProfession;
    private String userAppointmentsDuration;


    public PacketMainLogin(Activity _activity) {
        mActivity = _activity;
        mFirebaseFirestore = FirebaseFirestore.getInstance();
    }

    public void getUserDetails(@NonNull final FirebaseUser currentUser) {
        DocumentReference documentReference = mFirebaseFirestore.collection("users").document(currentUser.getUid());
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "Success");
                        userPhoneNumber = document.get("phoneNumber") != null ? document.get("phoneNumber").toString() : null;
                        userProfession = document.get("profession") != null ? document.get("profession").toString() : null;
                        userWorkingHoursID = document.get("workingDaysID") != null ? document.get("workingDaysID").toString() : null;
                        userAppointmentsDuration = document.get("appointmentsDuration") != null ? document.get("appointmentsDuration").toString() : null;

                        if(userWorkingHoursID == null) {
                            addUserWorkingDaysID(currentUser);
                        }
                        else {
                            redirectUser(currentUser);
                        }
                    } else {
                        userPhoneNumber = null;
                        userProfession = null;
                        addUserToDatabase(currentUser);
                        addUserWorkingDaysID(currentUser);
                    }
                } else {
                    userPhoneNumber = null;
                    userProfession = null;
                    redirectUser(currentUser);
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void checkWorkingDaysSetup(FirebaseUser currentUser) {
        final FirebaseUser localUser = currentUser;
        mFirebaseFirestore.collection("workingDays")
                .document(userWorkingHoursID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if(document.getData().containsValue(null)) {
                            getToInitActivity(localUser);
                        }
                        else {
                            getToCalendarActivity(localUser);
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
                        userWorkingHoursID = documentReference.getId();
                        userToAdd.put("workingDaysID", userWorkingHoursID);
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
        if(userPhoneNumber == null || userProfession == null) {
            getToInitActivity(localUser);
        }
        else {
            checkWorkingDaysSetup(localUser);
        }
    }


    private void getToInitActivity(FirebaseUser user) {
        if(userPhoneNumber == null) {
            Intent firstStep = new Intent(mActivity, SetPhoneNumberActivity.class);
            firstStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(firstStep, SPN_CANCEL);
        }
        else
        if(userProfession == null) {
            Intent secondStep = new Intent(mActivity, SetProffesionActivity.class);
            secondStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(secondStep, SP_CANCEL);
        } else {
            Intent thirdStep = new Intent(mActivity, SetWorkingHoursActivity.class);
            thirdStep.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(thirdStep, SWH_CANCEL);
        }
    }

    private void getToCalendarActivity(FirebaseUser user) {
        if(userAppointmentsDuration == null) {
            Intent ScheduleDuration = new Intent(mActivity, ScheduleDurationActivity.class);
            ScheduleDuration.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(ScheduleDuration, SD_CANCEL);
        }
        else {
            Intent CalendarActivity = new Intent(mActivity, com.example.schedly.CalendarActivity.class);
            CalendarActivity.putExtra("userID", user.getUid());
            mActivity.startActivityForResult(CalendarActivity, CA_CANCEL);
        }
    }
}
