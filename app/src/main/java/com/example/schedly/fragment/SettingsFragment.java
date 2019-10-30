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
import com.example.schedly.model.LogOut;
import com.example.schedly.model.NetworkChecker;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.example.schedly.setting.AppointmentDuration;
import com.example.schedly.setting.DisplayName;
import com.example.schedly.setting.PhoneNumber;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;


import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference mChangeEmailPreference;
    private Preference mChangePasswordPreference, mChangeWorkingHours;
    private Preference mChangePhoneNumber, mChangeDisplayName, mChangeAppointmentsDuration;
    private String mUserID, mUserPhoneNumber, mUserDisplayName;
    private String mUserAppointmentsDuration;
    private Preference mFeedback;
    private SwitchPreference mDisableMonitorization;
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
        enablePasswordEmailChange();
        mUserID = FirebaseAuth.getInstance().getUid();
        getDataFromDataBase();
    }

    private void enablePasswordEmailChange() {
        mChangeEmailPreference = findPreference("email_change");
        mChangePasswordPreference = findPreference("password_change");
        for (UserInfo user : FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
            if (user.getProviderId().equals("password")) {
                setVisibilityOnPreferences(true);
                mChangeEmailPreference.setSummary(user.getEmail());
                break;
            } else {
                setVisibilityOnPreferences(false);
            }
        }
    }

    private void setVisibilityOnPreferences(boolean visible) {
        mChangeEmailPreference.setVisible(visible);
        mChangePasswordPreference.setVisible(visible);
    }


    @Override
    public void onResume() {
        super.onResume();

        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_bar_title));
        getDataFromDataBase();
        getUserBlockedList();
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
        setLogOut();
    }

    private void setLogOut() {
        Preference preference = findPreference("logout");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogOut _logOut = new LogOut(mActivity);
                _logOut.LogOutFromApp();
                return false;
            }
        });
    }

    private void setMonitorization() {
        mDisableMonitorization = findPreference("stopNotificationSMSMonitoring");
        assert mDisableMonitorization != null;
        mDisableMonitorization.setChecked(MonitorIncomingSMSService.sServiceRunning);
        mDisableMonitorization.setSummary("Disable SMS monitoring");
        if(!NetworkChecker.isNetworkAvailable(mActivity)) {
            mDisableMonitorization.setEnabled(false);
        }
        mDisableMonitorization.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if (!(Boolean) newValue) {
                    displayAlertMonitorization();
                } else {
                    if(NetworkChecker.isNetworkAvailable(mActivity)) {
                        startServiceMonitoring();
                    }
                }
                return true;
            }
        });
    }

    private void startServiceMonitoring() {
        Intent serviceIntent = getIntentForService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mActivity.startForegroundService(serviceIntent);
        } else {
            mActivity.startService(serviceIntent);
        }
        setPreferenceForActiveMonitoring(true);
    }

    private void setPreferenceForActiveMonitoring(boolean value) {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        SharedPreferences.Editor _userEditor = _userPreferences.edit();
        _userEditor.putBoolean("serviceActive", value);
        _userEditor.apply();
    }

    private Intent getIntentForService() {
        Intent _intent = new Intent(mActivity, MonitorIncomingSMSService.class);
        _intent.putExtra("userID", ((SettingsActivity) mActivity).getmUserID());
        _intent.putExtra("userAppointmentDuration", ((SettingsActivity) mActivity).getmUserAppointmentDuration());
        _intent.putExtra("userWorkingHours", ((SettingsActivity) mActivity).getmWorkingHours());
        return _intent;
    }

    private void displayAlertMonitorization() {
        new AlertDialog.Builder(mActivity)
                .setTitle("Disable SMS monitoring")
                .setMessage(mActivity.getString(R.string.fragment_SMS_monitoring))
                .setIcon(R.drawable.ic_baseline_cancel_24px)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
                        mActivity.stopService(stopServiceIntent);
                        mDisableMonitorization.setSummary("Enable SMS monitoring");
                        setPreferenceForActiveMonitoring(false);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDisableMonitorization.setChecked(true);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void setBlockListPreference() {
        mBlockList = findPreference("block_list_access");
        assert mBlockList != null;
        mBlockList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startFragment(new BlockListFragment(mUserID));
                return false;
            }
        });
    }

    private void startFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frag_Settings_FL_Holder, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void setFeedbackPreference() {
        mFeedback = findPreference("help_feedback");
        assert mFeedback != null;
        mFeedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startSendEmailIntent();
                return true;
            }
        });
    }

    private void startSendEmailIntent() {
        Intent _sendFeedback = new Intent(Intent.ACTION_SEND);
        String[] recipients = {"TeamSchedly@gmail.com"};
        _sendFeedback.putExtra(Intent.EXTRA_EMAIL, recipients);
        _sendFeedback.setType("text/plain");
        _sendFeedback.setPackage("com.google.android.gm");
        mActivity.startActivity(_sendFeedback);
    }

    private void setDurationPreference() {
        mChangeAppointmentsDuration = findPreference("appointmentDuration_change");
        assert mChangeAppointmentsDuration != null;
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
        assert mChangeWorkingHours != null;
        mChangeWorkingHours.setFragment("com.example.schedly.ChangeWorkingDaysFragment");
        mChangeWorkingHours.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startFragment(new ChangeWorkingDaysFragment());
                return true;
            }
        });
    }

    private void setDisplayNamePreference() {
        mChangeDisplayName = findPreference("displayName_change");
        assert mChangeDisplayName != null;
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
                startFragment(new ChangePasswordFragment());
                return true;
            }
        });
    }

    private void setEmailPreference() {
        mChangeEmailPreference.setFragment("com.example.schedly.ChangeEmailFragment");
        mChangeEmailPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startFragment(new ChangeEmailFragment());
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
        final DocumentReference _docRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(mUserID);
        mRegistration = _docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ERR", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    getSnapshotValues(snapshot);
                }
            }
        });
    }

    private void getSnapshotValues(DocumentSnapshot snapshot) {
        mUserPhoneNumber = snapshot.get("phoneNumber").toString();
        mUserDisplayName = snapshot.get("displayName").toString();
        mUserAppointmentsDuration = snapshot.get("appointmentsDuration").toString();
        if (mPreferencesCreated) {
            setPreferencesForCurrentUser();
        }
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
    }

    public void setmUserPhoneNumber(String phoneNumber) {
        mUserPhoneNumber = phoneNumber;
    }
}
