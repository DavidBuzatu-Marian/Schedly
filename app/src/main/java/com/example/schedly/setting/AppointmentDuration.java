package com.example.schedly.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.example.schedly.R;
import com.example.schedly.fragment.SettingsFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AppointmentDuration {

    private Activity mActivity;
    private Preference mPreference;
    private SettingsFragment mSettingsFragment;


    public AppointmentDuration(Activity activity, Preference preference, SettingsFragment fragment) {
        mActivity = activity;
        mPreference = preference;
        mSettingsFragment = fragment;
        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        final View _dialogLayout = inflater.inflate(R.layout.dialog_settings_appointment_duration, null);
        builder.setView(_dialogLayout);
        builder.setTitle(mActivity.getString(R.string.dialog_app_duration_title));
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText _editText = _dialogLayout.findViewById(R.id.dialog_settings_appointment_duration);
                String _duration = _editText.getText().toString();
                if(!_duration.equals("")) {
                    saveAppointmentDuration(_duration);
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.dialog_app_change_empty), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog _dialog = builder.create();
        _dialog.show();


    }

    private void saveAppointmentDuration(final String duration) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        String _userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> _userToAdd = new HashMap<>();
        _userToAdd.put("appointmentsDuration", duration);
        mFireStore.collection("users")
                .document(_userID)
                .update(_userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mActivity, mActivity.getString(R.string.dialog_app_change_success), Toast.LENGTH_SHORT).show();
                        mPreference.setSummary(duration);
                        mSettingsFragment.setmUserAppointmentDuration(duration);
                    }
                });
    }
}
