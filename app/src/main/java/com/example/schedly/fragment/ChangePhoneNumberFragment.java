package com.example.schedly.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

public class ChangePhoneNumberFragment extends Fragment {

    private CountryCodePicker mCCP;
    private EditText mEditTextCarrierNumber;
    private boolean mValidNumber = false;
    private FragmentActivity mActivity;
    private View mInflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_phonenumber, container, false);
        return mInflater;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Change phone number");
    }

    @Override
    public void onStart() {
        super.onStart();

        mCCP = mInflater.findViewById(R.id.frag_CPNumber_cpp);
        mEditTextCarrierNumber = mInflater.findViewById(R.id.frag_CPNumber_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mCCP.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });
        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CPNumber_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mValidNumber) {
                    saveNewPhoneNumberInDatabase(mCCP.getFullNumberWithPlus());
                } else {
                    Toast.makeText(mActivity, "Phone number is invalid!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveNewPhoneNumberInDatabase(String fullNumber) {
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
                        getFragmentManager().popBackStack();
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
