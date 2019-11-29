package com.davidbuzatu.schedly.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.fragment.SettingsFragment;
import com.davidbuzatu.schedly.model.User;
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
        User.getInstance().updateUserAppointmentDuration(duration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mActivity, mActivity.getString(R.string.dialog_app_change_success), Toast.LENGTH_SHORT).show();
                        mPreference.setSummary(duration);
                    }
                });
    }
}
