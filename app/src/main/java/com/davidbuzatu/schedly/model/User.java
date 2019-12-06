package com.davidbuzatu.schedly.model;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.ScheduleDurationActivity;
import com.davidbuzatu.schedly.SetPhoneNumberActivity;
import com.davidbuzatu.schedly.SetProfessionActivity;
import com.davidbuzatu.schedly.SetWorkingHoursActivity;
import com.davidbuzatu.schedly.packet_classes.PacketMainLogin;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class User {
    private static User user = null;
    private static DocumentSnapshot userInfo;
    private static DocumentSnapshot userWorkingHours;
    private FirebaseUser firebaseUser;
    private HashMap<String, String> userWorkingHoursMap;

    private User(FirebaseUser user) {
        firebaseUser = user;
//        getUserWorkingHoursFromDB(user);
//        getUserInfo(user);
    }

    public Task<DocumentSnapshot> getUserInfo() {
        Task<DocumentSnapshot> task = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid()).get();
        task.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                userInfo = task.getResult();
            }
        });
        return task;
    }

    public String toString() {
        return userInfo.getData().toString();
    }

    public static User getInstance() {
        if (user == null) {
            user = new User(FirebaseAuth.getInstance().getCurrentUser());
        }
        return user;
    }

    public static void deleteInstance() {
        if (user != null) {
            user = null;
        }
    }

    public String getUid() {
        return firebaseUser.getUid();
    }

    public String getUserPhoneNumber() {
        Object phoneNumber = null;
        if(userInfo != null) {
            phoneNumber = userInfo.get(ContextForStrings.getContext().getString(R.string.user_phone_number));
        }
        return phoneNumber == null ? null : phoneNumber.toString();
    }

    public String getUserProfession() {
        Object profession = null;
        if(userInfo != null) {
            profession = userInfo.get(ContextForStrings.getContext().getString(R.string.user_profession));
        }
        return profession == null ? null : profession.toString();
    }


    public Task<DocumentSnapshot> getUserWorkingHoursFromDB() {
        Task<DocumentSnapshot> task = FirebaseFirestore.getInstance().collection("workingDays")
                .document(firebaseUser.getUid())
                .get();

        task.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().getData() != null) {
                    userWorkingHours = task.getResult();
                    setWorkingHoursMap();
                } else {
                    userWorkingHours = null;
                    userWorkingHoursMap = null;
                }
            }
        });
        return task;
    }

    private void setWorkingHoursMap() {
        Map<String, Object> workingHours = userWorkingHours.getData();
        userWorkingHoursMap = new HashMap<>();
        for (Map.Entry<String, Object> _entry : workingHours.entrySet()) {
            userWorkingHoursMap.put(_entry.getKey(), _entry.getValue().toString());
        }
    }

    public HashMap<String, String> getUserWorkingHours() {
        return userWorkingHoursMap;
    }

    public String getUserDisplayName() {
        Object displayName = userInfo.get(ContextForStrings.getContext().getString(R.string.user_display_name));
        return displayName == null ? null : displayName.toString();
    }

    public String getUserAppointmentsDuration() {
        Object appointmentsDuration = userInfo.get(ContextForStrings.getContext().getString(R.string.user_appointments_duration));
        return appointmentsDuration == null ? null : appointmentsDuration.toString();
    }


    public Task<Void> setUserPhoneNumber(String phoneNumber) {
        Map<String, Object> userPhoneNumberMap = new HashMap<>();
        userPhoneNumberMap.put("phoneNumber", phoneNumber);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .set(userPhoneNumberMap, SetOptions.merge());
    }

    public Task<Void> updateUserPhoneNumber(String phoneNumber) {
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", phoneNumber);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .update(userToAdd);
    }

    public Task<Void> updateUserDisplayName(String name) {
        Map<String, Object> _userToAdd = new HashMap<>();
        _userToAdd.put("displayName", name);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .update(_userToAdd);
    }

    public Task<Void> updateUserAppointmentDuration(String duration) {
        Map<String, Object> _userToAdd = new HashMap<>();
        _userToAdd.put("appointmentsDuration", duration);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .update(_userToAdd);
    }

    public Task<Void> setUserProfession(String profession) {
        Map<String, Object> userProfessionMap = new HashMap<>();
        userProfessionMap.put("profession", profession);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .update(userProfessionMap);

    }

    public Task<Void> setUserWorkingHours(Map<String, Object> daysToAdd) {
        return FirebaseFirestore.getInstance().collection("workingDays")
                .document(getUid())
                .set(daysToAdd);
    }

    public Task<Void> setUserAppointmentDuration(String minutes, String name) {
        Map<String, Object> durationAndName = new HashMap<>();
        durationAndName.put("appointmentsDuration", minutes);
        durationAndName.put("displayName", name);
        return FirebaseFirestore.getInstance().collection("users")
                .document(getUid())
                .update(durationAndName);
    }

    public void setUserInfo(DocumentSnapshot result) {
        userInfo = result;
    }
}
