package com.example.schedly.fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.service.MonitorIncomingSMSService.SERVICE_ID;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference mChangeEmailPreference;
    private Preference mChangePasswordPreference, mChangeWorkingHours;
    private Preference mChangePhoneNumber, mChangeDisplayName, mChangeAppointmentsDuration;
    private String mUserID, mUserPhoneNumber, mUserDisplayName, mUserWorkingHoursID;
    private String mUserAppointmentsDuration;
    private GoogleSignInClient mGoogleSignInClient;
    private FragmentActivity mActivity = getActivity();
    private boolean mPreferencesCreated = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        mPreferencesCreated = true;
        /* for logout */
        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();

        Preference preference = findPreference("logout");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Build a GoogleSignInClient with the options specified by gso.
                mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
                mGoogleSignInClient.signOut();
                LoginManager.getInstance().logOut();
                FirebaseAuth.getInstance().signOut();
                Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                stopServiceIntent.setAction("ACTION.STOPFOREGROUND_ACTION");
                mActivity.startService(stopServiceIntent);
                getActivity().setResult(LOG_OUT);
                getActivity().finish();
                return false;
            }
        });

        /* Enable password and email change if user
         * is logged in with account made with
         * email ( not facebook or google)
         */
        mChangeEmailPreference = findPreference("email_change");
        mChangePasswordPreference = findPreference("password_change");
        for (UserInfo user: FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
            if (user.getProviderId().equals("password")) {
                mChangeEmailPreference.setVisible(true);
                mChangePasswordPreference.setVisible(true);
                mChangeEmailPreference.setSummary(user.getEmail());
                break;
            }
            else {
                mChangeEmailPreference.setVisible(false);
                mChangePasswordPreference.setVisible(false);
            }
        }
        mUserID = FirebaseAuth.getInstance().getUid();

        /* Change display name, duration of appointments,
         * schedule of the working days,
         * phone-number
         */
        getDataFromDataBase();
    }

    @Override
    public void onResume() {
        super.onResume();

        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Settings");
        getDataFromDataBase();
    }

    private void setPreferencesForCurrentUser() {
        mChangePhoneNumber = findPreference("phoneNumber_change");
        mChangePhoneNumber.setSummary(mUserPhoneNumber);
        mChangePhoneNumber.setFragment("com.example.schedly.ChangePhoneNumberFragment");
        mChangePhoneNumber.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new ChangePhoneNumberFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });

        mChangeEmailPreference.setFragment("com.example.schedly.ChangeEmailFragment");
        mChangeEmailPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new ChangeEmailFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });

        mChangePasswordPreference.setFragment("com.example.schedly.ChangePasswordFragment");
        mChangePasswordPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new ChangePasswordFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });

        mChangeDisplayName = findPreference("displayName_change");
        mChangeDisplayName.setSummary(mUserDisplayName);
        mChangeDisplayName.setFragment("com.example.schedly.ChangeDisplayNameFragment");
        mChangeDisplayName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new ChangeDisplayNameFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });

        mChangeWorkingHours = findPreference("workingHours_change");
        mChangeWorkingHours.setFragment("com.example.schedly.ChangeWorkingDaysFragment");
        mChangeWorkingHours.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new ChangeWorkingDaysFragment(mUserWorkingHoursID);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });
    }

    private void getDataFromDataBase() {
        Log.d("CurrentUserSettings", mUserID);
        FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = _firebaseFirestore.collection("users").document(mUserID);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        mUserPhoneNumber = document.get("phoneNumber").toString();
//                        mUserProfessio = document.get("profession") != null ? document.get("profession").toString() : null;
                        mUserWorkingHoursID = document.get("workingDaysID").toString();
                        mUserDisplayName = document.get("displayName").toString();
                        mUserAppointmentsDuration = document.get("appointmentsDuration").toString();
                        if(mPreferencesCreated) {
                            setPreferencesForCurrentUser();
                        }
                    }
                }
            }
        });
    }

}
