package com.example.schedly.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.schedly.R;
import com.example.schedly.SetWorkingHoursActivity;
import com.example.schedly.SettingsActivity;
import com.example.schedly.packet_classes.PacketLinearLayoutSettings;
import com.example.schedly.packet_classes.PacketSpinnerViewSettings;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeWorkingDaysFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private ArrayAdapter<CharSequence> mAdapterHours;
    private PacketSpinnerViewSettings mPacketSpinnerViewSettings;
    private PacketLinearLayoutSettings mLinearLayoutSettings;
    private String mUserWorkingDaysID;

    public ChangeWorkingDaysFragment(String _workingDaysID) {
        mUserWorkingDaysID = _workingDaysID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_workingdays, container, false);
        return mInflater;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Change working days");
    }

    @Override
    public void onStart() {
        super.onStart();

        mAdapterHours = ArrayAdapter.createFromResource(getContext(), R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLinearLayoutSettings = new PacketLinearLayoutSettings(getContext(), mInflater);

        mPacketSpinnerViewSettings = new PacketSpinnerViewSettings(getContext(), mUserWorkingDaysID, mInflater, mAdapterHours);

        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CWHours_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewWorkingDaysInDatabase();
            }
        });
    }

    private void saveNewWorkingDaysInDatabase() {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> daysToAdd = mPacketSpinnerViewSettings.getDaysToAdd();

        mFireStore.collection("workingDays")
                .document(mUserWorkingDaysID)
                .update(daysToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Settings", "DocumentSnapshot successfully written!");
                        getFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Settings", "Error writing document", e);
                    }
                });
    }
}
