package com.davidbuzatu.schedly.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.activity.StartSplashActivity;
import com.davidbuzatu.schedly.service.MonitorIncomingSMSService;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import static com.davidbuzatu.schedly.model.ContextForStrings.getContext;

public class LogOut {

    private Context mContext;
    private Activity mActivity;
    private GoogleSignInClient mGoogleSignInClient;

    public LogOut(Activity activity) {
        mContext = getContext();
        mActivity = activity;
    }

    public void LogOutFromApp(boolean preferenceChanged) {
        logOutFromFirebase();
        stopService();
        Intent _loginIntent = getIntentForLogOut(preferenceChanged);
        mContext.startActivity(_loginIntent);
        mActivity.finishAffinity();
    }

    private Intent getIntentForLogOut(boolean preferenceChanged) {
        Intent _loginIntent = new Intent(mActivity, StartSplashActivity.class);
        _loginIntent.putExtra("LoggedOut", true);
        _loginIntent.putExtra("PreferenceChanged", preferenceChanged);
        _loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return _loginIntent;
    }

    private void logOutFromFirebase() {
        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(mContext.getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mActivity, gso);
        mGoogleSignInClient.signOut();
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        User.getInstance().deleteInstance();
    }

    private void stopService() {
        Intent _stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
        mActivity.stopService(_stopServiceIntent);
    }

}
