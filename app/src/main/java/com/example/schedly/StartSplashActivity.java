package com.example.schedly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.schedly.packet_classes.PacketMainLogin;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jakewharton.threetenabp.AndroidThreeTen;

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;
import static com.example.schedly.MainActivity.SD_CANCEL;
import static com.example.schedly.MainActivity.SPN_CANCEL;
import static com.example.schedly.MainActivity.SP_CANCEL;
import static com.example.schedly.MainActivity.SWH_CANCEL;
import static com.example.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class StartSplashActivity extends AppCompatActivity {

    FirebaseFirestore mFirebaseFirestore;
    private GoogleSignInClient mGoogleSignInClient;
    private int mResultCode = 0, mRequestCode = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);

        Bundle _extras = getIntent().getExtras();
        Log.d("Extras", _extras != null ? _extras.getBoolean("LoggedOut") + "" : "Null");
        if(_extras != null && _extras.getBoolean("LoggedOut")) {
            mResultCode = LOG_OUT;
            redirectWithScreenSize();
            finish();
        }
        /* preference check for first login
         * this is used in calendar activity
         * in order to display helpers
         */
        boolean _isPreferenceSet = getFirstLoginPreference();
        if(!_isPreferenceSet) {
            setFirstLoginPreference();
        }
        checkLoggedIn();
    }

    private void checkLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        /* check if user is logged, redirect accordingly */
        if(currentUser != null) {
            /* get user info and redirect */
            PacketMainLogin _packetMainLogin = new PacketMainLogin(this, false);
            _packetMainLogin.getUserDetails(currentUser);
        }
        else {
            /* get screen size and redirect to corresponding main */
            redirectWithScreenSize();
        }
    }

    private void setFirstLoginPreference() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor _userEditor = _userPreferences.edit();
        _userEditor.putBoolean("firstLogin", true);
        _userEditor.apply();
    }

    private boolean getFirstLoginPreference() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return _userPreferences.contains("firstLogin");
    }

    private void redirectWithScreenSize() {
        DisplayMetrics _displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(_displayMetrics);
        int _height = _displayMetrics.heightPixels;
        Intent _intentMainActivity = new Intent(this, MainActivity.class);
        _intentMainActivity.putExtra("resultCode", mResultCode);
        _intentMainActivity.putExtra("requestCode", mRequestCode);
        _intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if(_height < 1350) {
            _intentMainActivity.putExtra("SmallHeight", R.layout.activity_login_xsmall_devices);
        }
        startActivity(_intentMainActivity);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPN_CANCEL || requestCode == SP_CANCEL || requestCode == SWH_CANCEL || requestCode == SD_CANCEL) {
            final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_ID))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
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
