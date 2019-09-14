package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.schedly.packet_classes.PacketMainLogin;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.schedly.CalendarActivity.LOG_OUT;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager = null;
    private final String TAG = "RES";

    /* CREATED ACCOUNT WITH EMAIL */
    public static final int SUWEmailSuccess = 2006;
    /* FAILED ACCOUNT WITH EMAIL */
    public static final int SUWEmailFail = 2100;
    /* google sign in code */
    public static final int RC_SIGN_IN = 1001;
    public static final int SUWESuccess = 2000;
    /* first step back code */
    public static final int SPN_CANCEL = 2001;
    /* second step back code */
    public static final int SP_CANCEL = 2002;
    /* third step back code */
    public static final int SWH_CANCEL = 2003;
    /* last step back code */
    public static final int SD_CANCEL = 2004;
    /* calendar back press */
    public static final int CA_CANCEL = 2005;
    /* email changed */
    public static final int EMAIL_CHANGED = 4002;
    /* password changed */
    public static final int PASSWORD_CHANGED = 4003;
    /* working days changed */
    public static final int WORKING_HOURS_CHANGED = 4004;
    /* password reset */
    public static final int PR_SUCCESS = 4010;
    /* firestore */
    FirebaseFirestore mFirebaseFirestore;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean mShowPasswordTrue = false;
    private PacketMainLogin mPacketMainLogin;
    private View mDialogView = null;
    private int mResultCode, mRequestCode;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
        mFirebaseFirestore = FirebaseFirestore.getInstance();
//        if(currentUser != null) {
//            Log.d("Firebase", "Logged");
//            /* get user info and redirect */
//            mPacketMainLogin.getUserDetails(currentUser);
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle _extras = getIntent().getExtras();
        if (_extras != null && _extras.containsKey("SmallHeight")) {
            setContentView(_extras.getInt("SmallHeight"));
            mRequestCode = _extras.getInt("requestCode");
            mResultCode = _extras.getInt("resultCode");
            responseToResult(mRequestCode, mResultCode, null);
            setButtonsOnClick();
            /* for sign up */
        } else {
            setContentView(R.layout.activity_login);
            setUpSocialsLogin();
            setUpEmailLogin();
        }

        final TextView buttonSignUpMain = findViewById(R.id.act_main_TV_SingUp);
        buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivityForResult(signUpIntent, SUWEmailSuccess);
            }
        });

        mPacketMainLogin = new PacketMainLogin(this, true);
        mAuth = FirebaseAuth.getInstance();

    }

    private void setUpEmailLogin(final View view) {
        /* for sign up */
        final TextView buttonSignUpMain = view.findViewById(R.id.act_main_TV_SingUp);
        buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivity(signUpIntent);
            }
        });
        /* for password reset */
        final TextView buttonForgotPassword = view.findViewById(R.id.act_main_TV_ForgotPassword);
        buttonForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _forgotPassword = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                TextInputEditText _txtInputEmail = view.findViewById(R.id.act_main_TIET_email);
                _forgotPassword.putExtra("Email", _txtInputEmail.getText().toString());
                startActivityForResult(_forgotPassword, PR_SUCCESS);
            }
        });
        /* for show password */
        final ImageView imageViewShowPassword = view.findViewById(R.id.act_main_IV_ShowPassword);
        imageViewShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextPass = view.findViewById(R.id.act_main_TIET_password);
                if (mShowPasswordTrue) {
                    editTextPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imageViewShowPassword.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_24px));
                    editTextPass.setSelection(editTextPass.getText().length());
                    mShowPasswordTrue = false;
                } else {
                    editTextPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    imageViewShowPassword.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_off_24px));
                    editTextPass.setSelection(editTextPass.getText().length());
                    mShowPasswordTrue = true;
                }
            }
        });

        /* for sign in */
        final Button buttonSignInMain = view.findViewById(R.id.act_main_BUT_signin);
        buttonSignInMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true);
                EditText editTextEmail = view.findViewById(R.id.act_main_TIET_email);
                EditText editTextPass = view.findViewById(R.id.act_main_TIET_password);
                TextInputLayout textInputLayoutEmail = view.findViewById(R.id.act_main_TIL_email);
                TextInputLayout textInputLayoutPass = view.findViewById(R.id.act_main_TIL_password);
                boolean errorFoundTrue = false;
                final String ET_Email = editTextEmail.getText().toString();
                final String ET_Password = editTextPass.getText().toString();
                if (ET_Email.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutEmail.setError("Field required!");
                    mPacketMainLogin.showProgressBar(false);
                } else {
                    textInputLayoutEmail.setErrorEnabled(false);
                }
                if (ET_Password.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutPass.setError("Filed required!");
                    mPacketMainLogin.showProgressBar(false);
                } else {
                    textInputLayoutPass.setErrorEnabled(false);
                }
                if (!errorFoundTrue) {
                    signInWithEmailAndPassword(ET_Email, ET_Password);
                }
            }
        });
    }

    private void setUpEmailLogin() {
        /* for password reset */
        final TextView buttonForgotPassword = findViewById(R.id.act_main_TV_ForgotPassword);
        buttonForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _forgotPassword = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                TextInputEditText _txtInputEmail = findViewById(R.id.act_main_TIET_email);
                _forgotPassword.putExtra("Email", _txtInputEmail.getText().toString());
                startActivityForResult(_forgotPassword, PR_SUCCESS);
            }
        });
        /* for show password */
        final ImageView imageViewShowPassword = findViewById(R.id.act_main_IV_ShowPassword);
        imageViewShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextPass = findViewById(R.id.act_main_TIET_password);
                if (mShowPasswordTrue) {
                    editTextPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imageViewShowPassword.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_24px));
                    editTextPass.setSelection(editTextPass.getText().length());
                    mShowPasswordTrue = false;
                } else {
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
                mPacketMainLogin.showProgressBar(true);
                EditText editTextEmail = findViewById(R.id.act_main_TIET_email);
                EditText editTextPass = findViewById(R.id.act_main_TIET_password);
                TextInputLayout textInputLayoutEmail = findViewById(R.id.act_main_TIL_email);
                TextInputLayout textInputLayoutPass = findViewById(R.id.act_main_TIL_password);
                boolean errorFoundTrue = false;
                final String ET_Email = editTextEmail.getText().toString();
                final String ET_Password = editTextPass.getText().toString();
                if (ET_Email.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutEmail.setError("Field required!");
                    mPacketMainLogin.showProgressBar(false);
                } else {
                    textInputLayoutEmail.setErrorEnabled(false);
                }
                if (ET_Password.isEmpty()) {
                    errorFoundTrue = true;
                    textInputLayoutPass.setError("Filed required!");
                    mPacketMainLogin.showProgressBar(false);
                } else {
                    textInputLayoutPass.setErrorEnabled(false);
                }
                if (!errorFoundTrue) {
                    signInWithEmailAndPassword(ET_Email, ET_Password);
                }
            }
        });
    }

    private void setUpSocialsLogin(View view) {
        /* FACEBOOK LOGIN */
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton loginButtonFacebook = view.findViewById(R.id.buttonFacebookLogin);
        loginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                mPacketMainLogin.showProgressBar(true);
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

        view.findViewById(R.id.act_main_BUT_Google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true);
                signIn();
            }
        });
    }

    private void setUpSocialsLogin() {
        /* FACEBOOK LOGIN */
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton loginButtonFacebook = findViewById(R.id.buttonFacebookLogin);
        loginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                mPacketMainLogin.showProgressBar(true);
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
                mPacketMainLogin.showProgressBar(true);
                signIn();
            }
        });
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
                            mPacketMainLogin.getUserDetails(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d("successWithCredential", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, R.string.loginFail,
                                    Toast.LENGTH_SHORT).show();
                            mPacketMainLogin.showProgressBar(false);
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
            mPacketMainLogin.showProgressBar(false);
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
                            Log.d(TAG, "signInWithCredential:success GOOGLE");
                            @NonNull FirebaseUser user = mAuth.getCurrentUser();
                            mPacketMainLogin.getUserDetails(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, R.string.loginFail,
                                    Toast.LENGTH_SHORT).show();
                            mPacketMainLogin.showProgressBar(false);
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
                            @NonNull FirebaseUser user = mAuth.getCurrentUser();
                            mPacketMainLogin.getUserDetails(user);
                        } else {
                            mPacketMainLogin.showProgressBar(false);
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidCredentialsException ex) {
                                TextInputLayout _textInputLayoutPass;
                                if (mDialogView != null) {
                                    _textInputLayoutPass = mDialogView.findViewById(R.id.act_main_TIL_password);
                                } else {
                                    _textInputLayoutPass = findViewById(R.id.act_main_TIL_password);
                                }
                                _textInputLayoutPass.setError(getText(R.string.loginFailPasswordIncorrect));
                            } catch (FirebaseAuthInvalidUserException ex) {
                                final TextInputEditText _txtInputEmail;
                                if (mDialogView != null) {
                                    _txtInputEmail = mDialogView.findViewById(R.id.act_main_TIL_password);
                                } else {
                                    _txtInputEmail = findViewById(R.id.act_main_TIL_password);
                                }
                                Snackbar.make(findViewById(R.id.act_main_CL_Root), "Email not registered. Sign up here", Snackbar.LENGTH_LONG)
                                        .setAction("Sign Up", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Intent _signUpIntent = new
                                                        Intent(MainActivity.this, SignUpWithEmailActivity.class);
                                                _signUpIntent.putExtra("Email", _txtInputEmail.getText().toString());
                                                startActivity(_signUpIntent);
                                            }
                                        }).show();
                            } catch (FirebaseTooManyRequestsException ex) {
                                Toast.makeText(MainActivity.this, R.string.loginFailTooManyReq,
                                        Toast.LENGTH_SHORT).show();
                            } catch (Exception ex) {
                                Toast.makeText(MainActivity.this, R.string.loginFail,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    public void doLoginFacebook(View view) {
        LoginButton loginButton;
        if (mDialogView == null) {
            loginButton = findViewById(R.id.buttonFacebookLogin);
        } else {
            loginButton = mDialogView.findViewById(R.id.buttonFacebookLogin);
        }
        loginButton.performClick();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCallbackManager != null) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
        responseToResult(requestCode, resultCode, data);
    }

    private void setButtonsOnClick() {
        Button _btnSocials = findViewById(R.id.act_main_BUT_signin_options_socials);
        Button _btnEmail = findViewById(R.id.act_main_BUT_signin_options_email);

        _btnSocials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callLoginSocialsDialog();
            }
        });

        _btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callLoginEmailDialog();
            }
        });
    }

    private void callLoginEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View _dialogLayout = inflater.inflate(R.layout.dialog_login_small_height_email, null);
        builder.setView(_dialogLayout);
        builder.setTitle("SIGN IN");
        builder.setMessage("Use the credentials which you used for sign up");
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mDialogView = null;
                mPacketMainLogin.setDialogViewExists(false);
            }
        });
        mDialogView = _dialogLayout;
        mPacketMainLogin.setDialogViewExists(true);
        setUpEmailLogin(_dialogLayout);
    }

    private void callLoginSocialsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View _dialogLayout = inflater.inflate(R.layout.dialog_login_small_height_socials, null);
        builder.setView(_dialogLayout);
        builder.setTitle("SIGN IN");
        builder.setMessage("Choose one method from below: ");
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mDialogView = null;
                mPacketMainLogin.setDialogViewExists(false);
            }
        });
        mDialogView = _dialogLayout;
        mPacketMainLogin.setDialogViewExists(true);
        setUpSocialsLogin(_dialogLayout);

    }
    private void  responseToResult(int requestCode, int resultCode, Intent data ) {
        Log.d("REQUEST", requestCode + ";" + resultCode);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == SPN_CANCEL || requestCode == SP_CANCEL || requestCode == SWH_CANCEL || requestCode == SD_CANCEL) {
            mPacketMainLogin.showProgressBar(false);
            mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "All fields are required!", Toast.LENGTH_LONG).show();
        } else {
            switch (resultCode) {
                case SUWEmailSuccess:
                    Toast.makeText(MainActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();
                    PacketMainLogin _packetMainLogin = new PacketMainLogin(this, false);
                    _packetMainLogin.getUserDetails(mAuth.getCurrentUser());
                    break;
                case EMAIL_CHANGED:
                    Toast.makeText(MainActivity.this, "Email changed. Please login again.", Toast.LENGTH_LONG).show();
                    break;
                case PASSWORD_CHANGED:
                    Toast.makeText(MainActivity.this, "Password changed. Please login again.", Toast.LENGTH_LONG).show();
                    break;
                case WORKING_HOURS_CHANGED:
                    Toast.makeText(MainActivity.this, "Working hours changed. Please login again.", Toast.LENGTH_LONG).show();
                    break;
                case PR_SUCCESS:
                    Snackbar.make(findViewById(R.id.act_main_CL_Root), "An email with instructions for your password reset was sent", Snackbar.LENGTH_LONG).show();
                    break;
                case LOG_OUT:
                    mPacketMainLogin.showProgressBar(false);
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mPacketMainLogin.showProgressBar(false);
    }
}
