package com.davidbuzatu.schedly.setting;

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

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.fragment.SettingsFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

public class PhoneNumber {

    private Activity mActivity;
    private EditText mPhoneNumberET;
    private String mPhoneNumber;
    private Preference mPreference;
    private SettingsFragment mSettingsFragment;
    private CountryCodePicker mCCP;
    private EditText mEditTextCarrierNumber;
    private boolean mValidNumber = false;

    public PhoneNumber(Activity activity, String phoneNumber, Preference preference, SettingsFragment fragment) {
        mActivity = activity;
        mPhoneNumber = phoneNumber;
        mPreference = preference;
        mSettingsFragment = fragment;
        buildDialog();
    }

    private void buildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        final View _dialogLayout = inflater.inflate(R.layout.dialog_settings_phone_number, null);
        builder.setView(_dialogLayout);
        builder.setTitle(mActivity.getString(R.string.dialog_phone_number_title));
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mValidNumber) {
                    savePhoneNumber(mCCP.getFullNumberWithPlus());
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.dialog_phone_number_error), Toast.LENGTH_SHORT).show();
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
        setElements(_dialogLayout);
        mPhoneNumberET = _dialogLayout.findViewById(R.id.dialog_settings_PN_CPNumber_ET_carrierNumber);
        mPhoneNumberET.setHint(mPhoneNumber);
    }

    private void savePhoneNumber(String fullNumber) {
        mPhoneNumber = fullNumber;
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        String _userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", fullNumber);
        mFireStore.collection("users")
                .document(_userID)
                .update(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mActivity, mActivity.getString(R.string.dialog_pnumber_change_success), Toast.LENGTH_SHORT).show();
                        mPreference.setSummary(mPhoneNumber);
                        mSettingsFragment.setmUserPhoneNumber(mPhoneNumber);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Change", "Error writing document", e);
                    }
                });
    }

    private void setElements(View dialogLayout) {
        mCCP = dialogLayout.findViewById(R.id.dialog_settings_PN_CPNumber_cpp);
        mEditTextCarrierNumber = dialogLayout.findViewById(R.id.dialog_settings_PN_CPNumber_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mCCP.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });
    }
}
