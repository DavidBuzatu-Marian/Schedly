package com.davidbuzatu.schedly;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.davidbuzatu.schedly.model.LogOut;
import com.davidbuzatu.schedly.model.User;
import com.davidbuzatu.schedly.packet_classes.PacketMainLogin;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.jakewharton.threetenabp.AndroidThreeTen;

import static com.davidbuzatu.schedly.CalendarActivity.LOG_OUT;
import static com.davidbuzatu.schedly.MainActivity.EMAIL_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.PASSWORD_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.WORKING_HOURS_CHANGED;
import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;

public class StartSplashActivity extends AppCompatActivity {
    private int mResultCode = 0, mRequestCode = 0;
    private boolean preferenceChanged;
    private boolean[] fetched = new boolean[2];
    private final int UPDATE_REQUEST_CODE = 100001;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForUpdate();
        AndroidThreeTen.init(this);
        Bundle _extras = getIntent().getExtras();
        if(_extras != null && _extras.getBoolean("LoggedOut")) {
            mResultCode = LOG_OUT;
            preferenceChanged = _extras.getBoolean("PreferenceChanged");
            redirectWithScreenSize();
            finish();
        }
        if(!isFirstLoginPreferenceSet()) {
            setFirstLoginPreference();
        }
        checkLoggedIn();
    }

    private void checkForUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                IMMEDIATE,
                                StartSplashActivity.this,
                                UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        new OnSuccessListener<AppUpdateInfo>() {
                            @Override
                            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                                if (appUpdateInfo.updateAvailability()
                                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                    try {
                                        appUpdateManager.startUpdateFlowForResult(
                                                appUpdateInfo,
                                                IMMEDIATE,
                                                StartSplashActivity.this,
                                                UPDATE_REQUEST_CODE);
                                    } catch (IntentSender.SendIntentException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
    }

    private void checkLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            redirectWithScreenSize();
            return;
        }

        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.addContentView(progressBar, params);

        final User user = User.getInstance();
        user.getUserInfo().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    user.setUserInfo(task.getResult());
                }
                fetched[0] = true;
                if (isFinishedFetching()) {
                    progressBar.setVisibility(View.GONE);
                    PacketMainLogin.redirectUser(StartSplashActivity.this);
                }
            }
        });
        user.getUserWorkingHoursFromDB().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[1] = true;
                if(isFinishedFetching()) {
                    progressBar.setVisibility(View.GONE);
                    PacketMainLogin.redirectUser(StartSplashActivity.this);
                }
            }
        });
    }

    private boolean isFinishedFetching() {
        for(Boolean b : fetched) {
            if(!b) {
                return false;
            }
        }
        return true;
    }

    private void setFirstLoginPreference() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor _userEditor = _userPreferences.edit();
        _userEditor.putBoolean("firstLogin", true);
        _userEditor.apply();
    }

    private boolean isFirstLoginPreferenceSet() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return _userPreferences.contains("firstLogin");
    }

    private void redirectWithScreenSize() {
        DisplayMetrics _displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(_displayMetrics);
        int _height = _displayMetrics.heightPixels;
        Intent _intentMainActivity = getIntentMainActivity(_height);
        startActivity(_intentMainActivity);
        finish();
    }

    private Intent getIntentMainActivity(int height) {
        Intent _intentMainActivity = new Intent(this, MainActivity.class);
        _intentMainActivity.putExtra("resultCode", mResultCode);
        _intentMainActivity.putExtra("requestCode", mRequestCode);
        _intentMainActivity.putExtra("PreferenceChanged", preferenceChanged);
        _intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if(height < 1350) {
            _intentMainActivity.putExtra("SmallHeight", R.layout.activity_login_xsmall_devices);
        }
        return _intentMainActivity;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case LOG_OUT:
            case EMAIL_CHANGED:
            case PASSWORD_CHANGED:
            case WORKING_HOURS_CHANGED:
                mResultCode = resultCode;
                redirectWithScreenSize();
                finish();
        }
    }

}
