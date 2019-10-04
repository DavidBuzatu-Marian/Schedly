package com.example.schedly.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.schedly.StartSplashActivity;
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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;


import java.util.Map;

import static com.example.schedly.CalendarActivity.LOG_OUT;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference mChangeEmailPreference;
    private Preference mChangePasswordPreference, mChangeWorkingHours;
    private Preference mChangePhoneNumber, mChangeDisplayName, mChangeAppointmentsDuration;
    private String mUserID, mUserPhoneNumber, mUserDisplayName, mUserWorkingHoursID;
    private String mUserAppointmentsDuration;
    private Preference mFeedback;
    private SwitchPreference mDisableMonitorization;
    private GoogleSignInClient mGoogleSignInClient;
    private FragmentActivity mActivity = getActivity();
    private boolean mPreferencesCreated = false;
    private Map<String, Object> mBlockedNumbers;
    private Preference mBlockList;
    private ListenerRegistration mRegistration;
    private ListenerRegistration mRegistrationBlock;

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
                mActivity.stopService(stopServiceIntent);
                getActivity().setResult(LOG_OUT);
                Intent loginIntent = new Intent(mActivity, StartSplashActivity.class);
                loginIntent.putExtra("LoggedOut", true);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(loginIntent);
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
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_bar_title));
        getDataFromDataBase();
    }

    @Override
    public void onStop() {
        super.onStop();

        mRegistration.remove();
        mRegistrationBlock.remove();
    }

    private void setPreferencesForCurrentUser() {
        setPhoneNumberPreference();

        setEmailPreference();

        setPasswordPreference();

        setDisplayNamePreference();

        setWorkingHoursPreference();
        setDurationPreference();
        setFeedbackPreference();

        setBlockListPreference();
        setMonitorization();
    }

    private void setMonitorization() {
        mDisableMonitorization = findPreference("stopNotificationSMSMonitoring");
        mDisableMonitorization.setChecked(MonitorIncomingSMSService.sServiceRunning);
        mDisableMonitorization.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(!(Boolean) newValue) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Disable SMS monitoring")
                            .setMessage(mActivity.getString(R.string.fragment_SMS_monitoring))
                            .setIcon(R.drawable.ic_baseline_cancel_24px)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                                    mActivity.stopService(stopServiceIntent);
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
                            })
                            .setCancelable(false)
                            .show();
                }
                else {
                    Intent serviceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                    serviceIntent.putExtra("userID", ((SettingsActivity) mActivity).getmUserID());
                    serviceIntent.putExtra("userAppointmentDuration", ((SettingsActivity) mActivity).getmUserAppointmentDuration());
                    serviceIntent.putExtra("userWorkingDaysID", ((SettingsActivity) mActivity).getmUserWorkingDaysID());
                    serviceIntent.putExtra("userWorkingHours", ((SettingsActivity) mActivity).getmWorkingHours());
                    serviceIntent.setAction("ACTION.STARTSERVICE_ACTION");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mActivity.startForegroundService(serviceIntent);
                    } else {
                        mActivity.startService(serviceIntent);
                    }
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

    private void setBlockListPreference() {
        mBlockList = findPreference("block_list_access");
        mBlockList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment _newFragment = new BlockListFragment(mUserID);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_Settings_FL_Holder, _newFragment);
                transaction.addToBackStack(null);
                transaction.commit();

                return false;
            }
        });
    }

    private void setFeedbackPreference() {
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
    }

    private void setDurationPreference() {
        mChangeAppointmentsDuration = findPreference("appointmentDuration_change");
        mChangeAppointmentsDuration.setSummary(mUserAppointmentsDuration);
        mChangeAppointmentsDuration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AppointmentDuration(mActivity, mChangeAppointmentsDuration, SettingsFragment.this);
                return true;
            }
        });
    }

    private void setWorkingHoursPreference() {
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

    private void setDisplayNamePreference() {
        mChangeDisplayName = findPreference("displayName_change");
        mChangeDisplayName.setSummary(mUserDisplayName);
        mChangeDisplayName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DisplayName(mActivity, mUserDisplayName, mChangeDisplayName, SettingsFragment.this);
                return true;
            }
        });
    }

    private void setPasswordPreference() {
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
    }

    private void setEmailPreference() {
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
    }

    private void setPhoneNumberPreference() {
        mChangePhoneNumber = findPreference("phoneNumber_change");
        mChangePhoneNumber.setSummary(mUserPhoneNumber);
        mChangePhoneNumber.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new PhoneNumber(mActivity, mUserPhoneNumber, mChangePhoneNumber, SettingsFragment.this);
                return true;
            }
        });
    }

    private void getDataFromDataBase() {
        final DocumentReference _docRef = FirebaseFirestore.getInstance().collection("users")
                .document(mUserID);
        mRegistration = _docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ERR", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    mUserPhoneNumber = snapshot.get("phoneNumber").toString();
                    mUserWorkingHoursID = snapshot.get("workingDaysID").toString();
                    mUserDisplayName = snapshot.get("displayName").toString();
                    mUserAppointmentsDuration = snapshot.get("appointmentsDuration").toString();
                    getUserBlockedList();
                    if(mPreferencesCreated) {
                        setPreferencesForCurrentUser();
                    }
                }
            }
        });
    }

    private void getUserBlockedList() {
        final DocumentReference _docRef = FirebaseFirestore.getInstance().collection("blockLists")
                .document(mUserID);
        mRegistrationBlock = _docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ERR", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    mBlockedNumbers = snapshot.getData();
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
