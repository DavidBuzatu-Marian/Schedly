package com.example.schedly.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.example.schedly.setting.AppointmentDuration;
import com.example.schedly.setting.DisplayName;
import com.example.schedly.setting.PhoneNumber;
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


import static com.example.schedly.CalendarActivity.LOG_OUT;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference mChangeEmailPreference;
    private Preference mChangePasswordPreference, mChangeWorkingHours;
    private Preference mChangePhoneNumber, mChangeDisplayName, mChangeAppointmentsDuration;
    public String mUserID, mUserPhoneNumber, mUserDisplayName, mUserWorkingHoursID;
    public String mUserAppointmentsDuration;
    private Preference mFeedback;
    private SwitchPreference mDisableMonitorization;
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
        mChangePhoneNumber.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new PhoneNumber(mActivity, mUserPhoneNumber, mChangePhoneNumber, SettingsFragment.this);
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
        mChangeDisplayName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DisplayName(mActivity, mUserDisplayName, mChangeDisplayName, SettingsFragment.this);
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
        mChangeAppointmentsDuration = findPreference("appointmentDuration_change");
        mChangeAppointmentsDuration.setSummary(mUserAppointmentsDuration);
        mChangeAppointmentsDuration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AppointmentDuration(mActivity, mChangeAppointmentsDuration, SettingsFragment.this);
                return true;
            }
        });


        mFeedback = findPreference("help_feedback");
        mFeedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent _sendFeedback = new Intent(Intent.ACTION_SEND);
                String[] recipients={"TeamSchedly@gmail.com"};
                _sendFeedback.putExtra(Intent.EXTRA_EMAIL, recipients);
                _sendFeedback.setType("text/plain");
                _sendFeedback.setPackage("com.google.android.gm");
                mActivity.startActivity(_sendFeedback);
                return true;
            }
        });

        mDisableMonitorization = findPreference("stopNotificationSMSMonitoring");
        mDisableMonitorization.setChecked(MonitorIncomingSMSService.sServiceRunning);
        mDisableMonitorization.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(!(Boolean) newValue) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Disable SMS monitoring")
                            .setMessage("Do you really want to disable SMS monitoring? Schedly will NOT be able to make appointments through SMS if disabled")
                            .setIcon(R.drawable.ic_baseline_cancel_24px)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                                    stopServiceIntent.setAction("ACTION.STOPFOREGROUND_ACTION");
                                    mActivity.startService(stopServiceIntent);
                                    mDisableMonitorization.setSummary("Enable SMS monitoring");

                                    SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
                                    SharedPreferences.Editor _userEditor = _userPreferences.edit();
                                    _userEditor.putBoolean("serviceActive", false);
                                    _userEditor.apply();
                                }})
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mDisableMonitorization.setChecked(true);
                                }
                            }).show();
                }
                else {
                    Intent serviceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                    serviceIntent.putExtra("userID", ((SettingsActivity) mActivity).getmUserID());
                    serviceIntent.putExtra("userDaysWithScheduleID", ((SettingsActivity) mActivity).getmUserDaysWithScheduleID());
                    serviceIntent.putExtra("userAppointmentDuration", ((SettingsActivity) mActivity).getmUserAppointmentDuration());
                    serviceIntent.putExtra("userWorkingDaysID", ((SettingsActivity) mActivity).getmUserWorkingDaysID());
                    serviceIntent.setAction("ACTION.STARTSERVICE_ACTION");
                    mActivity.startService(serviceIntent);
                    mDisableMonitorization.setSummary("Disable SMS monitoring");

                    SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    SharedPreferences.Editor _userEditor = _userPreferences.edit();
                    _userEditor.putBoolean("serviceActive", true);
                    _userEditor.apply();
                }
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

    public void setmUserDisplayName(String name) {
        mUserDisplayName = name;
    }

    public void setmUserAppointmentDuration(String duration) {
        mUserAppointmentsDuration = duration;
        Log.d("SetDuration", mUserAppointmentsDuration);
    }

    public void setmUserPhoneNumber(String phoneNumber) {
        mUserPhoneNumber = phoneNumber;
    }
}
