package com.davidbuzatu.schedly.model;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;


public class ScheduledHours {

    private static ScheduledHours scheduledHours;
    private DocumentSnapshot scheduledHoursSnapshot;

    public static ScheduledHours getInstance() {
        if(scheduledHours == null) {
            scheduledHours = new ScheduledHours();
        }
        return scheduledHours;
    }


    public Task<DocumentSnapshot> getScheduledHours(String userID) {
        Task<DocumentSnapshot> task = FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(userID)
                .get();
        scheduledHoursSnapshot = task.isSuccessful() ? task.getResult() : null;
        return task;
    }

    public Task<Void> saveScheduledHour(String name, String phoneNumber, String appointmentType, String appointmentHour, String appointmentDate, String userID) {
        Map<String, Object> _appointment = setDataForAppointmentSave(name, phoneNumber, appointmentType, appointmentHour, appointmentDate);
        return FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(userID)
                .set(_appointment, SetOptions.merge());
    }

    private Map<String, Object> setDataForAppointmentSave(String name, String phoneNumber, String appointmentType, String appointmentHour, String appointmentDate) {
        Map<String, String> _detailsOfAppointment = new HashMap<>();
        _detailsOfAppointment.put("PhoneNumber", phoneNumber);
        _detailsOfAppointment.put("Name", name.equals("") ? null : name);
        _detailsOfAppointment.put("AppointmentType", appointmentType);
        Map<String, Object> _hourAndInfo = new HashMap<>();
        _hourAndInfo.put(appointmentHour, _detailsOfAppointment);
        Map<String, Object> _appointment = new HashMap<>();
        _appointment.put(appointmentDate, _hourAndInfo);
        return _appointment;
    }

    public DocumentSnapshot getScheduledHoursSnapshot() {
        return scheduledHoursSnapshot;
    }

    public Task<Void> removeAppointment(Map<String, Object> details, Long mDateInMillis) {
        Map<String, Object> _appointmentMap = new HashMap<>();
        _appointmentMap.put(mDateInMillis.toString(), details);
        return FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(User.getInstance().getUid())
                .set(_appointmentMap, SetOptions.merge());
    }
}
