package com.example.schedly.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.schedly.R;
import com.example.schedly.fragment.SettingsFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DisplayName {

    private Activity mActivity;
    private EditText mDisplayNameEditText;
    private String mDisplayName;
    private Preference mPreference;
    private SettingsFragment mSettingsFragment;

    public DisplayName(Activity activity, String displayName, Preference preference, SettingsFragment fragment) {
        mActivity = activity;
        mDisplayName = displayName;
        mPreference = preference;
        mSettingsFragment = fragment;
        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        final View _dialogLayout = inflater.inflate(R.layout.dialog_settings_display_name, null);
        builder.setView(_dialogLayout);
        builder.setTitle(mActivity.getString(R.string.dialog_display_name_title));
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String _name = mDisplayNameEditText.getText().toString();
                if(validName(_name)) {
                    saveDisplayName(_name);
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
        mDisplayNameEditText = _dialogLayout.findViewById(R.id.dialog_settings_display_name);
        mDisplayNameEditText.setHint(mDisplayName);
    }

    private boolean validName(String name) {
        if(name.length() > 28 || name.length() < 1) {
            mDisplayNameEditText.setError(mActivity.getString(R.string.dialog_display_name_validity));
            return false;
        }
        return true;
    }

    private void saveDisplayName(final String newDisplayName) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        String _userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("displayName", newDisplayName);
        mFireStore.collection("users")
                .document(_userID)
                .update(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mActivity, mActivity.getString(R.string.dialog_display_name_success), Toast.LENGTH_SHORT).show();
                        mPreference.setSummary(newDisplayName);
                        mSettingsFragment.setmUserDisplayName(newDisplayName);
                        Log.d("Change", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Change", "Error writing document", e);
                    }
                });
    }
}
