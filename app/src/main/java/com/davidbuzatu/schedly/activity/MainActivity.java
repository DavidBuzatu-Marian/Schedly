package com.davidbuzatu.schedly.activity;

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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.User;
import com.davidbuzatu.schedly.packet_classes.PacketMainLogin;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
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
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;

import static com.davidbuzatu.schedly.activity.CalendarActivity.LOG_OUT;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager = null;
    private final String TAG = "MainActivity";

    /* CREATED ACCOUNT WITH EMAIL */
    public static final int SUWEmailSuccess = 2006;
    /* FAILED ACCOUNT WITH EMAIL */
    public static final int SUWEmailFail = 2100;
    /* google sign in code */
    public static final int RC_SIGN_IN = 1001;
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
    private GoogleSignInClient mGoogleSignInClient;
    private boolean mShowPasswordTrue = false;
    private PacketMainLogin mPacketMainLogin;
    private View mDialogView = null;
    private int mResultCode, mRequestCode;
    private AlertDialog mDialogError;
    private boolean[] fetched = new boolean[2];

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle _extras = getIntent().getExtras();
        if (_extras != null && _extras.containsKey("SmallHeight")) {
            setContentView(_extras.getInt("SmallHeight"));
            mRequestCode = _extras.getInt("requestCode");
            mResultCode = _extras.getInt("resultCode");
            if(_extras.getBoolean("PreferenceChanged")) {
                Toast.makeText(this, "Changed saved. Please log in again!", Toast.LENGTH_LONG).show();
            }
            responseToResult(mRequestCode, mResultCode, null);
            setButtonsOnClick();
            /* for sign up */
        } else {
            if(_extras != null && _extras.getBoolean("PreferenceChanged")) {
                Toast.makeText(this, "Changed saved. Please log in again!", Toast.LENGTH_LONG).show();
            }
            setContentView(R.layout.activity_login);
            setUpSocialsLogin();
            setUpEmailLogin();
        }
        setUpTVMain();

        mPacketMainLogin = new PacketMainLogin();
    }

    private void setUpTVMain() {
        final TextView _buttonSignUpMain = findViewById(R.id.act_main_TV_SingUp);
        _buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivityForResult(signUpIntent, SUWEmailSuccess);
            }
        });
    }

    private void setUpEmailLogin(final View view) {
        signUpSetUp(view);
        forgotPasswordSetUp(view);
        showPasswordSetUp(view);
        signInSetUp(view);
    }

    private void signInSetUp(final View view) {
        final Button buttonSignInMain = view.findViewById(R.id.act_main_BUT_signin);
        buttonSignInMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                EditText editTextEmail = view.findViewById(R.id.act_main_TIET_email);
                EditText editTextPass = view.findViewById(R.id.act_main_TIET_password);
                TextInputLayout textInputLayoutEmail = view.findViewById(R.id.act_main_TIL_email);
                TextInputLayout textInputLayoutPass = view.findViewById(R.id.act_main_TIL_password);
                final String ET_Email = editTextEmail.getText().toString();
                final String ET_Password = editTextPass.getText().toString();
                checkForErrorsOnLogin(textInputLayoutEmail, textInputLayoutPass, ET_Email, ET_Password);
            }
        });
    }

    private void showPasswordSetUp(final View view) {
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
    }

    private void forgotPasswordSetUp(final View view) {
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
    }

    private void signUpSetUp(View view) {
        final TextView _buttonSignUpMain = view.findViewById(R.id.act_main_TV_SingUp);
        _buttonSignUpMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                startActivity(_signUpIntent);
            }
        });
    }

    private void setUpEmailLogin() {
        forgotPasswordSetUp();
        showPasswordSetUp();
        signInSetUp();
    }

    private void signInSetUp() {
        final Button _buttonSignInMain = findViewById(R.id.act_main_BUT_signin);
        _buttonSignInMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                EditText _editTextEmail = findViewById(R.id.act_main_TIET_email);
                EditText _editTextPass = findViewById(R.id.act_main_TIET_password);
                TextInputLayout _textInputLayoutEmail = findViewById(R.id.act_main_TIL_email);
                TextInputLayout _textInputLayoutPass = findViewById(R.id.act_main_TIL_password);
                final String _ET_Email = _editTextEmail.getText().toString();
                final String _ET_Password = _editTextPass.getText().toString();
                checkForErrorsOnLogin(_textInputLayoutEmail, _textInputLayoutPass, _ET_Email, _ET_Password);
            }
        });
    }

    private void checkForErrorsOnLogin(TextInputLayout textInputLayoutEmail, TextInputLayout textInputLayoutPass, String ET_Email, String ET_Password) {
        boolean errorFoundTrue = false;
        if (ET_Email.isEmpty()) {
            errorFoundTrue = true;
            textInputLayoutEmail.setError("Field required!");
            mPacketMainLogin.showProgressBar(false, MainActivity.this);
        } else {
            textInputLayoutEmail.setErrorEnabled(false);
        }
        if (ET_Password.isEmpty()) {
            errorFoundTrue = true;
            textInputLayoutPass.setError("Filed required!");
            mPacketMainLogin.showProgressBar(false, MainActivity.this);
        } else {
            textInputLayoutPass.setErrorEnabled(false);
        }
        if (!errorFoundTrue) {
            signInWithEmailAndPassword(ET_Email, ET_Password);
        }
    }

    private void showPasswordSetUp() {
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
    }

    private void forgotPasswordSetUp() {
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
    }

    private void setUpSocialsLogin(View view) {
        mCallbackManager = CallbackManager.Factory.create();
        facebookLogin(view);
        googleLogin(view);
    }

    private void googleLogin(View view) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        view.findViewById(R.id.act_main_BUT_Google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                signIn();
            }
        });
    }

    private void facebookLogin(View view) {
        LoginButton loginButtonFacebook = view.findViewById(R.id.buttonFacebookLogin);
        loginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                loginErrorDialog();
            }

            @Override
            public void onError(FacebookException error) {
                loginErrorDialog();
            }

        });
    }

    private void setUpSocialsLogin() {
        mCallbackManager = CallbackManager.Factory.create();
        facebookLogin();
        googleLogin();
    }


    private void facebookLogin() {
        LoginButton loginButtonFacebook = findViewById(R.id.buttonFacebookLogin);
        loginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                loginErrorDialog();
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("ErrFacebook", error.toString());
                loginErrorDialog();
            }
        });
    }

    private void loginErrorDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View _dialogLayout = inflater.inflate(R.layout.dialog_login_error, null);
        createAlertDialog(_dialogLayout);
    }

    private void createAlertDialog(View dialogLayout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogLayout);
        mDialogError = builder.create();
        mDialogError.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogError.show();
    }


    private void googleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.act_main_BUT_Google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketMainLogin.showProgressBar(true, MainActivity.this);
                signIn();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            initUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            loginErrorDialog();
                            mPacketMainLogin.showProgressBar(false, MainActivity.this);
                        }
                    }
                });
    }

    private void initUser() {
        User user = User.getInstance();
        user.getUserInfo().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[0] = true;
                if(isFinishedFetching()) {
                    mPacketMainLogin.showProgressBar(false, MainActivity.this);
                    PacketMainLogin.redirectUser(MainActivity.this);
                }
            }
        });
        user.getUserWorkingHoursFromDB().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[1] = true;
                if(isFinishedFetching()) {
                    mPacketMainLogin.showProgressBar(false, MainActivity.this);
                    PacketMainLogin.redirectUser(MainActivity.this);
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


    /* GOOGLE SIGN IN */
    private void signIn() {
        Intent _signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(_signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount _account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(_account);
        } catch (ApiException e) {
            loginErrorDialog();
            Log.w(TAG, "handleSignInResult:error", e);
            mPacketMainLogin.showProgressBar(false, MainActivity.this);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            initUser();
                        } else {
                            loginErrorDialog();
                            mPacketMainLogin.showProgressBar(false, MainActivity.this);
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

                            initUser();
                        } else {
                            mPacketMainLogin.showProgressBar(false, MainActivity.this);
                            throwExceptions(task);
                        }
                    }
                });
    }

    private void throwExceptions(Task<AuthResult> task) {
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
                _txtInputEmail = mDialogView.findViewById(R.id.act_main_TIET_email);
            } else {
                _txtInputEmail = findViewById(R.id.act_main_TIET_email);
            }
            makeSnackBar(_txtInputEmail);
        } catch (FirebaseTooManyRequestsException ex) {
            Toast.makeText(MainActivity.this, R.string.loginFailTooManyReq, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, R.string.loginFail, Toast.LENGTH_SHORT).show();
        }
    }

    private void makeSnackBar(final TextInputEditText txtInputEmail) {
        Snackbar.make(findViewById(R.id.act_main_CL_Root), getString(R.string.activity_main_snackbar_email_not_registered), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.activity_main_snackbar_email_not_registered_action), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent _signUpIntent = new Intent(MainActivity.this, SignUpWithEmailActivity.class);
                        _signUpIntent.putExtra("Email", txtInputEmail.getText().toString());
                        startActivity(_signUpIntent);
                    }
                }).show();
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
        LayoutInflater inflater = getLayoutInflater();
        View _dialogLayout = inflater.inflate(R.layout.dialog_login_small_height_email, null);
        createAlertDialog(_dialogLayout, R.string.activity_main_small_socials_message_title, R.string.activity_main_small_credentials);
        setmDialogView(_dialogLayout);
        setUpEmailLogin(_dialogLayout);
    }

    private void setmDialogView(View dialogLayout) {
        mDialogView = dialogLayout;
        mPacketMainLogin.setDialogViewExists(true);
    }

    private void createAlertDialog(View dialogLayout,
                                   int title,
                                   int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogLayout);
        builder.setTitle(getString(title));
        builder.setMessage(getString(message));
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mDialogView = null;
                mPacketMainLogin.setDialogViewExists(false);
            }
        });
    }
    public void dismissDialogError(View view) {
        mDialogError.dismiss();
    }

    private void callLoginSocialsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View _dialogLayout = inflater.inflate(R.layout.dialog_login_small_height_socials, null);
        createAlertDialog(_dialogLayout, R.string.activity_main_small_socials_message_title, R.string.activity_main_small_socials_message);
        setmDialogView(_dialogLayout);
        setUpSocialsLogin(_dialogLayout);

    }

    private void responseToResult(int requestCode, int resultCode, Intent data) {
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        switchOnResult(resultCode);

    }

    private void switchOnResult(int resultCode) {
        switch (resultCode) {
            case SUWEmailSuccess:
                Toast.makeText(MainActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();
                PacketMainLogin.redirectUser(this);
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
                mPacketMainLogin.showProgressBar(false, MainActivity.this);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mPacketMainLogin.showProgressBar(false, MainActivity.this);
    }
}
