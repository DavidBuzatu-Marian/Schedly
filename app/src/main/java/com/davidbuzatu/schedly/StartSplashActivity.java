package com.davidbuzatu.schedly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.jakewharton.threetenabp.AndroidThreeTen;

import static com.davidbuzatu.schedly.CalendarActivity.LOG_OUT;
import static com.davidbuzatu.schedly.MainActivity.EMAIL_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.PASSWORD_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.SD_CANCEL;
import static com.davidbuzatu.schedly.MainActivity.SPN_CANCEL;
import static com.davidbuzatu.schedly.MainActivity.SP_CANCEL;
import static com.davidbuzatu.schedly.MainActivity.SWH_CANCEL;
import static com.davidbuzatu.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class StartSplashActivity extends AppCompatActivity {
    private int mResultCode = 0, mRequestCode = 0;
    private boolean[] fetched = new boolean[2];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        Bundle _extras = getIntent().getExtras();
        if(_extras != null && _extras.getBoolean("LoggedOut")) {
            mResultCode = LOG_OUT;
            redirectWithScreenSize();
            finish();
        }
        if(!isFirstLoginPreferenceSet()) {
            setFirstLoginPreference();
        }
        checkLoggedIn();
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

        User user = User.getInstance();
        user.getUserInfo(currentUser).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[0] = true;
                if(isFinishedFetching()) {
                    progressBar.setVisibility(View.GONE);
                    PacketMainLogin.redirectUser(StartSplashActivity.this);
                }
            }
        });
        user.getUserWorkingHoursFromDB(currentUser).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
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
        _intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if(height < 1350) {
            _intentMainActivity.putExtra("SmallHeight", R.layout.activity_login_xsmall_devices);
        }
        return _intentMainActivity;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPN_CANCEL || requestCode == SP_CANCEL || requestCode == SWH_CANCEL || requestCode == SD_CANCEL) {
            LogOut _logOut = new LogOut(this);
            _logOut.LogOutSetUp();
            mRequestCode = requestCode;
            redirectWithScreenSize();
            finish();
        }
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
