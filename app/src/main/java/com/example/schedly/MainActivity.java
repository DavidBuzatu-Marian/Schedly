package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager;
    private final String TAG = "RES";
    /* google sign in code */
    private final int RC_SIGN_IN = 1001;
    private final int SUWESuccess = 2000;
    /* first step back code */
    private final int PH_CANCEL = 2001;
    /* second step back code */
    private final int SP_CANCEL = 2002;
    /* third step back code */
    private final int SWH_CANCEL = 2003;
    /* last step back code */
    private final int SD_CANCEL = 2004;
    /* calendar back press */
    private final int CA_CANCEL = 2005;
    /* email changed */
    public static final int EMAIL_CHANGED = 4002;
    /* password changed */
    public static final int PASSWORD_CHANGED = 4003;
    /* firestore */
    FirebaseFirestore firebaseFirestore;
    /* store user info */
    private String userWorkingHoursID;
    private String userPhoneNumber;
    private String userProfession;
    private String userAppointmentsDuration;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean mShowPasswordTrue = false;


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        if(currentUser != null) {
            Log.d("Firebase", "Logged");
            /* get user info and redirect */
            getUserDetails(currentUser);

        }
//
//        // Check for existing Google Sign In account, if the user is already signed in
//        // the GoogleSignInAccount will be non-null.
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
//        if(account == null) {
//            Log.d("Google", "not logged");
//        }
//        else {
//
//        }
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
        final TextView buttonSignUpMain = findViewById(R.id.act_main_TV_SingUp);
        buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivity(signUpIntent);
            }
        });
        /* for password reset */
        final TextView buttonForgotPassword = findViewById(R.id.act_main_TV_ForgotPassword);
        buttonForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent forgotPassword = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(forgotPassword);
            }
        });
        /* for show password */
        final ImageView imageViewShowPassword = findViewById(R.id.act_main_IV_ShowPassword);
        imageViewShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextPass = findViewById(R.id.act_main_TIET_password);
                if(mShowPasswordTrue) {
                    editTextPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imageViewShowPassword.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_24px));
                    editTextPass.setSelection(editTextPass.getText().length());
                    mShowPasswordTrue = false;
                }
                else {
                    editTextPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    imageViewShowPassword.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_off_24px));
                    editTextPass.setSelection(editTextPass.getText().length());
                    mShowPasswordTrue = true;
                }
            }
        });

        /* for sign in */
        final Button buttonSignInMain = findViewById(R.id.act_main_BUT_signin);
        buttonSignInMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextEmail = findViewById(R.id.act_main_TIET_email);
                EditText editTextPass = findViewById(R.id.act_main_TIET_password);
                TextInputLayout textInputLayoutEmail = findViewById(R.id.act_main_TIL_email);
                TextInputLayout textInputLayoutPass = findViewById(R.id.act_main_TIL_password);
                boolean errorFoundTrue = false;
                final String ET_Email = editTextEmail.getText().toString();
                final String ET_Password = editTextPass.getText().toString();
                if(ET_Email.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutEmail.setError("Field required!");
                }
                else {
                    textInputLayoutEmail.setErrorEnabled(false);
                }
                if(ET_Password.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutPass.setError("Filed required!");
                }
                else {
                    textInputLayoutPass.setErrorEnabled(false);
                }
                if(!errorFoundTrue) {
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
        if(requestCode == PH_CANCEL || requestCode == SP_CANCEL || requestCode == SWH_CANCEL || requestCode == SD_CANCEL) {
            mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this,"All fields are required!", Toast.LENGTH_LONG).show();
        }
        if(requestCode == SUWESuccess) {
            Toast.makeText(MainActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        }
        if(resultCode == EMAIL_CHANGED) {
            Toast.makeText(MainActivity.this, "Email changed. Please login again.", Toast.LENGTH_LONG).show();
        }
        if(resultCode == PASSWORD_CHANGED) {
            Toast.makeText(MainActivity.this, "Password changed. Please login again.", Toast.LENGTH_LONG).show();
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
                            getUserDetails(user);
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
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            getUserDetails(user);
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
                            getUserDetails(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed. Are you registered?",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /* get phone number and domain of activity
    IF BOTH EXISTS -> it will go to calendar activity
    IF ONE DOESN'T -> it goes to one of the init activity
     */
    private void getUserDetails(@NonNull final FirebaseUser currentUser) {
        final FirebaseUser localUser = currentUser;
        DocumentReference documentReference = firebaseFirestore.collection("users").document(localUser.getUid());
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        userPhoneNumber = document.get("phoneNumber") != null ? document.get("phoneNumber").toString() : null;
                        userProfession = document.get("profession") != null ? document.get("profession").toString() : null;
                        userWorkingHoursID = document.get("workingDaysID") != null ? document.get("workingDaysID").toString() : null;
                        userAppointmentsDuration = document.get("appointmentsDuration") != null ? document.get("appointmentsDuration").toString() : null;

                        if(userWorkingHoursID == null) {
                            addUserWorkingDaysID(currentUser);
                        }
                        else {
                            redirectUser(localUser);
                        }
                    } else {
                        userPhoneNumber = null;
                        userProfession = null;
                        addUserToDatabase(currentUser);
                        addUserWorkingDaysID(currentUser);
                    }
                } else {
                    userPhoneNumber = null;
                    userProfession = null;
                    redirectUser(localUser);
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void checkWorkingDaysSetup(FirebaseUser currentUser) {
        final FirebaseUser localUser = currentUser;
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        mFireStore.collection("workingDays")
                .document(userWorkingHoursID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if(document.getData().containsValue(null)) {
                            getToInitActivity(localUser);
                        }
                        else {
                            getToCalendarActivity(localUser);
                        }
                    }
                });
    }

    private void addUserToDatabase(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", null);
        userToAdd.put("profession", null);
        db.collection("users")
                .document(user.getUid())
                .set(userToAdd);
    }

    private void addUserWorkingDaysID(final FirebaseUser currentUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        /* add days of the week to collection */
        Map<String, Object> daysOfTheWeek = new HashMap<>();
        daysOfTheWeek.put("MondayStart", null);
        daysOfTheWeek.put("MondayEnd", null);
        daysOfTheWeek.put("TuesdayStart", null);
        daysOfTheWeek.put("TuesdayEnd", null);
        daysOfTheWeek.put("WednesdayStart", null);
        daysOfTheWeek.put("WednesdayEnd", null);
        daysOfTheWeek.put("ThursdayStart", null);
        daysOfTheWeek.put("ThursdayEnd", null);
        daysOfTheWeek.put("FridayStart", null);
        daysOfTheWeek.put("FridayEnd", null);
        daysOfTheWeek.put("SaturdayStart", null);
        daysOfTheWeek.put("SaturdayEnd", null);
        daysOfTheWeek.put("SundayStart", null);
        daysOfTheWeek.put("SundayEnd", null);
        db.collection("workingDays")
                .add(daysOfTheWeek)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
                        Map<String, Object> userToAdd = new HashMap<>();
                        Log.d(TAG, "DocumentSnapshot written:" + documentReference.getId());
                        userWorkingHoursID = documentReference.getId();
                        userToAdd.put("workingDaysID", userWorkingHoursID);
                        mFireStore.collection("users")
                                .document(currentUser.getUid())
                                .update(userToAdd)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        redirectUser(currentUser);
                                        Log.d(TAG, "DocumentSnapshot successfully written!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void redirectUser(final FirebaseUser localUser) {
        /* REDIRECT */
        if(userPhoneNumber == null || userProfession == null) {
            getToInitActivity(localUser);
        }
        else {
            checkWorkingDaysSetup(localUser);
        }
    }


    private void getToInitActivity(FirebaseUser user) {
        if(userPhoneNumber == null) {
            Intent firstStep = new Intent(MainActivity.this, SetPhoneNumberActivity.class);
            firstStep.putExtra("userID", user.getUid());
            startActivityForResult(firstStep, PH_CANCEL);
        }
        else
            if(userProfession == null) {
            Intent secondStep = new Intent(MainActivity.this, SetProffesionActivity.class);
            secondStep.putExtra("userID", user.getUid());
            startActivityForResult(secondStep, SP_CANCEL);
        } else {
                Intent thirdStep = new Intent(MainActivity.this, SetWorkingHoursActivity.class);
                thirdStep.putExtra("userID", user.getUid());
                startActivityForResult(thirdStep, SWH_CANCEL);
            }
    }

    private void getToCalendarActivity(FirebaseUser user) {
        if(userAppointmentsDuration == null) {
            Intent ScheduleDuration = new Intent(MainActivity.this, ScheduleDurationActivity.class);
            ScheduleDuration.putExtra("userID", user.getUid());
            startActivityForResult(ScheduleDuration, SD_CANCEL);
        }
        else {
            Intent CalendarActivity = new Intent(MainActivity.this, CalendarActivity.class);
            CalendarActivity.putExtra("userID", user.getUid());
            startActivityForResult(CalendarActivity, CA_CANCEL);
        }
    }

    public void doLoginFacebook(View view) {
        LoginButton loginButton = findViewById(R.id.buttonFacebookLogin);
        loginButton.performClick();
    }
}
