package com.example.schedly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.schedly.packet_classes.PacketMainLogin;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.MainActivity.SD_CANCEL;
import static com.example.schedly.MainActivity.SPN_CANCEL;
import static com.example.schedly.MainActivity.SP_CANCEL;
import static com.example.schedly.MainActivity.SWH_CANCEL;

public class StartSplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseFirestore mFirebaseFirestore;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        if(currentUser != null) {
            Log.d("Firebase", "Logged");
            /* get user info and redirect */
            PacketMainLogin _packetMainLogin = new PacketMainLogin(this, false);
            _packetMainLogin.getUserDetails(currentUser);
        }
        else {
            Intent _intentMainActivity = new Intent(this, MainActivity.class);
            startActivity(_intentMainActivity);
            finish();
        }
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
            Intent _intentMainActivity = new Intent(this, MainActivity.class);
            startActivity(_intentMainActivity);
            finish();
        }
        if(resultCode == LOG_OUT) {
            Intent _intentMainActivity = new Intent(this, MainActivity.class);
            startActivity(_intentMainActivity);
            finish();
        }
    }

}
