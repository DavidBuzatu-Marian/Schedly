package com.example.schedly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.StartSplashActivity;
import com.example.schedly.model.DaysOfWeek;
import com.example.schedly.packet_classes.PacketCardViewSettings;
import com.example.schedly.packet_classes.PacketSpinnerViewSettings;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

import static com.example.schedly.MainActivity.PASSWORD_CHANGED;
import static com.example.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class ChangeWorkingDaysFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private ArrayAdapter<CharSequence> mAdapterHours;
    private PacketSpinnerViewSettings mPacketSpinnerViewSettings;
    private PacketCardViewSettings mCardViewSettings;
    private GoogleSignInClient mGoogleSignInClient;
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
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_working_days_bar_title));
    }

    @Override
    public void onStart() {
        super.onStart();

        mAdapterHours = ArrayAdapter.createFromResource(getContext(), R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCardViewSettings = new PacketCardViewSettings(getContext(), mInflater);

        mPacketSpinnerViewSettings = new PacketSpinnerViewSettings(getContext(), mUserWorkingDaysID, mInflater, mAdapterHours);

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
                        logOut();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Settings", "Error writing document", e);
                    }
                });
    }

    private void logOut() {
        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        mGoogleSignInClient.signOut();
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
        mActivity.stopService(stopServiceIntent);
        Intent loginIntent = new Intent(mActivity, StartSplashActivity.class);
        loginIntent.putExtra("LoggedOut", true);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
        getActivity().finish();
    }
}
