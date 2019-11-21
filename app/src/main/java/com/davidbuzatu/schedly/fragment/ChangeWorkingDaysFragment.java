package com.davidbuzatu.schedly.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.SettingsActivity;
import com.davidbuzatu.schedly.model.DaysOfWeek;
import com.davidbuzatu.schedly.model.LogOut;
import com.davidbuzatu.schedly.packet_classes.PacketCardViewSettings;
import com.davidbuzatu.schedly.packet_classes.PacketSpinnerViewSettings;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ChangeWorkingDaysFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private ArrayAdapter<CharSequence> mAdapterHours;
    private PacketSpinnerViewSettings mPacketSpinnerViewSettings;

    public ChangeWorkingDaysFragment() {
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
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_working_days_bar_title));
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapterHours = ArrayAdapter.createFromResource(getContext(), R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        PacketCardViewSettings mCardViewSettings = new PacketCardViewSettings(getContext(), mInflater);
        mPacketSpinnerViewSettings = new PacketSpinnerViewSettings(getContext(), mInflater, mAdapterHours);
        setUpSaveButton();
    }

    private void setUpSaveButton() {
        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CWHours_BUT_saveChanges);
        RelativeLayout.LayoutParams _layoutParamsBTN =  (RelativeLayout.LayoutParams) _saveChangesButton.getLayoutParams();
        _layoutParamsBTN.addRule(RelativeLayout.BELOW, DaysOfWeek.SUN.getCardViewId());
        _layoutParamsBTN.addRule(RelativeLayout.CENTER_HORIZONTAL);
        _saveChangesButton.setLayoutParams(_layoutParamsBTN);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewWorkingDaysInDatabase();
            }
        });
    }

    private void saveNewWorkingDaysInDatabase() {
        Map<String, Object> daysToAdd = mPacketSpinnerViewSettings.getDaysToAdd();
        FirebaseFirestore.getInstance().collection("workingDays")
                .document(FirebaseAuth.getInstance().getUid())
                .update(daysToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        getFragmentManager().popBackStack();
                        LogOut _logOut = new LogOut(mActivity);
                        _logOut.LogOutFromApp();
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
