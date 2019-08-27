package com.example.schedly.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeDisplayNameFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private EditText mDisplayNameEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_displayname, container, false);
        return mInflater;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Change display name");
    }

    @Override
    public void onStart() {
        super.onStart();

        mDisplayNameEditText = mInflater.findViewById(R.id.frag_CDName_ET_DisplayName);

        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CDName_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String _name = mDisplayNameEditText.getText().toString();
                if (validName(_name)) {
                    saveNew2DisplayNameInDatabase(_name);
                }
            }
        });
    }

    private boolean validName(String name) {
        if(name.length() > 28 || name.length() < 1) {
            mDisplayNameEditText.setError("Display name must have at least 1 character and at max 28");
            return false;
        }
        return true;
    }

    private void saveNewDisplayNameInDatabase(String newDisplayName) {
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
