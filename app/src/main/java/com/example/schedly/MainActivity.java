package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.hbb20.CountryCodePicker;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager;
    private final String TAG = "RES";
    /* google sign in code */
    private final int RC_SIGN_IN = 1001;
    /* first step back code */
    private final int PH_CANCEL = 2001;
    /* second step back code */
    private final int SP_CANCEL = 2002;
    /* calendar back press */
    private final int CA_CANCEL = 2003;
    private GoogleSignInClient mGoogleSignInClient;


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            Log.d("Firebase", "Logged");
            check_details(currentUser);
            if(currentUser.getPhoneNumber().isEmpty()) {
                Log.d("next_intent", "init");
                getToInitActivity(currentUser);
            }
            else {
                Log.d("next_intent", "calendar");
                getToCalendarActivity(currentUser);
            }
        }

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null) {
            Log.d("Google", "not logged");
        }
        else {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            if(currentUser.getPhoneNumber().isEmpty()) {
                getToInitActivity(currentUser);
            }
            else {
                getToCalendarActivity(currentUser);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();


        /* FACEBOOK LOGIN */
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButtonFacebook = findViewById(R.id.buttonFacebookLogin);
        loginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                // ...
            }

        });

        /* GOOGLE LOGIN */

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.act_main_BUT_Google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });




        /* for sign up */
        final Button buttonSignUpMain = findViewById(R.id.act_main_BUT_signup);
        buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivity(signUpIntent);
            }
        });

        /* for sign in */
        final Button buttonSignInMain = findViewById(R.id.act_main_BUT_signin);
        buttonSignInMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextEmail = findViewById(R.id.act_main_TIET_email);
                EditText editTextPass = findViewById(R.id.act_main_TIET_password);
                final String ET_Email = editTextEmail.getText().toString();
                final String ET_Password = editTextPass.getText().toString();
                if(ET_Email.isEmpty() || ET_Password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Empty fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    signInWithEmailAndPassword(ET_Email, ET_Password);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if(requestCode == PH_CANCEL || requestCode == SP_CANCEL) {
            mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this,"All fields are required!", Toast.LENGTH_LONG).show();
        }
        if(requestCode == CA_CANCEL) {
            this.finish();
            System.exit(0);
        }

    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("token_handler", "handleFacebookAccessToken:" + token.getToken());

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("successWithCredential", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if(!isNewUser) {
                                getToCalendarActivity(user);
                            }
                            else {
                                getToInitActivity(user);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d("successWithCredential", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    /* GOOGLE SIGN IN */
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            //String idToken = account.getIdToken();
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if(!isNewUser) {
                                getToCalendarActivity(user);
                            }
                            else {
                                getToInitActivity(user);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if(!isNewUser) {
                                getToCalendarActivity(user);
                            }
                            else {
                                getToInitActivity(user);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void check_details(FirebaseUser currentUser) {

        if (currentUser != null) {
            // Name, email address, and profile photo Url
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            String phnumber = currentUser.getPhoneNumber();

            // Check if user's email is verified
            boolean emailVerified = currentUser.isEmailVerified();


            Log.d("info", name);
            Log.d("info", email);
            Log.d("info", emailVerified ? "YES": "NO");
            Log.d("info", phnumber);
            Log.d("info", currentUser.getUid());
        }
    }


    private void getToInitActivity(FirebaseUser user) {
        if(user.getPhoneNumber() == null) {
            Intent firstStep = new Intent(MainActivity.this, SetPhoneNumberActivity.class);
            firstStep.putExtra("userID", user.getUid());
            startActivityForResult(firstStep, PH_CANCEL);
        }
        else {
            Intent secondStep = new Intent(MainActivity.this, SetProffesionActivity.class);
            secondStep.putExtra("userID", user.getUid());
            startActivityForResult(secondStep, SP_CANCEL);
        }
    }

    private void getToCalendarActivity(FirebaseUser user) {
        Intent CalendarActivity = new Intent(MainActivity.this, CalendarActivity.class);
        CalendarActivity.putExtra("userID", user.getUid());
        startActivityForResult(CalendarActivity, CA_CANCEL);
    }
}
