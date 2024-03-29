package com.davidbuzatu.schedly.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.User;
import com.davidbuzatu.schedly.packet_classes.PacketMainLogin;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.hbb20.CountryCodePicker;


import static com.davidbuzatu.schedly.activity.MainActivity.SUWEmailFail;


public class SignUpWithEmailActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private final String TAG = "SUWEmail";
    /* country getter */
    private CountryCodePicker mCCP;
    private final int SUWESuccess = 2000;
    private EditText mEditTextCarrierNumber;
    private TextInputLayout mTextInputLayoutEmail;
    private TextInputLayout mTextInputLayoutPass;
    private boolean mShowPasswordTrue = false;
    ProgressBar mProgressBar;
    private AlertDialog mDialogError;

    @Override
    public void onStart() {
        super.onStart();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_with_email);
        mProgressBar = findViewById(R.id.act_SUWEmail_PB);

        mCCP = findViewById(R.id.ccp);
        mEditTextCarrierNumber = findViewById(R.id.act_SUWEmail_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mAuth = FirebaseAuth.getInstance();
        mTextInputLayoutEmail = findViewById(R.id.act_SUWEmail_TIL_email);
        mTextInputLayoutPass = findViewById(R.id.act_SUWEmail_TIL_password);

        if (getIntent().hasExtra("Email")) {
            TextInputEditText _txtInputEmail = findViewById(R.id.act_SUWEmail_TIET_email);
            _txtInputEmail.setText(getIntent().getStringExtra("Email"));
        }
        setUpButtonSignUp();
        setUpFloatingButton();
        setUpShowPassword();
    }

    private void setUpShowPassword() {
        final ImageView imageViewShowPassword = findViewById(R.id.act_SUWEmail_IV_ShowPassword);
        imageViewShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextPass = findViewById(R.id.act_SUWEmail_TIET_password);
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

    private void setUpFloatingButton() {
        FloatingActionButton floatingActionButton = findViewById(R.id.act_SUWEmail_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(SUWEmailFail);
                SignUpWithEmailActivity.this.finish();
            }
        });
    }

    private void setUpButtonSignUp() {
        final Button buttonSignUp = findViewById(R.id.act_signup_BUT_signup);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText editTextEmail = findViewById(R.id.act_SUWEmail_TIET_email);
                final EditText editTextPass = findViewById(R.id.act_SUWEmail_TIET_password);
                if (inputValidation(editTextEmail.getText().toString(), editTextPass.getText().toString())) {
                    showProgressBar(true);
                    signUpWithEmailAndPassword(editTextEmail.getText().toString(), editTextPass.getText().toString());
                }
            }
        });
    }

    private boolean inputValidation(String email, String password) {
        /* email validation */
        if (!emailVerification(email)) {
            return false;
        }
        /* password validation */
        if (!passwordVerification(password)) {
            return false;
        }
        /* number is not okay */
        if (!mCCP.isValidFullNumber()) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return false;
        }
        /* validation is okay */
        return true;
    }

    private boolean passwordVerification(String password) {
        boolean hasDigit_TRUE = false;
        if (password.isEmpty()) {
            mTextInputLayoutPass.setError("Field required!");
            return false;
        }
        /* check if length is between limits */
        if (password.length() < 8 || password.length() > 20) {
            mTextInputLayoutPass.setError("Password must be between 8 and 20 characters");
            return false;
        }
        /* it has digits */
        for (char digit : password.toCharArray()) {
            if (Character.isDigit(digit)) {
                hasDigit_TRUE = true;
            }
        }
        if (!hasDigit_TRUE) {
            mTextInputLayoutPass.setError("Password must have digits");
            return false;
        }
        /* it has uppercase/ lowercase letter */
        if (password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            mTextInputLayoutPass.setError("Password needs lowercase and uppercase letters");
            return false;
        }
        mTextInputLayoutPass.setErrorEnabled(false);
        return true;
    }

    private boolean emailVerification(String email) {
        if (email.isEmpty()) {
            mTextInputLayoutEmail.setError("Field required!");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mTextInputLayoutEmail.setError("Invalid Email");
            return false;
        }
        mTextInputLayoutEmail.setErrorEnabled(false);
        return true;
    }

    private void signUpWithEmailAndPassword(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            User.getInstance().setUserPhoneNumber(mCCP.getFullNumberWithPlus())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            User.getInstance().getUserInfo().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    PacketMainLogin.redirectUser(SignUpWithEmailActivity.this);
                                                }
                                            });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                                showToastError();
                                        }
                                    });
                        } else {
                            throwExceptions(task);
                        }
                    }
                });
    }
    public void showToastError() {
        Toast.makeText(this, "Something went wrong! Please check your internet connection!", Toast.LENGTH_LONG).show();
    }

    private void throwExceptions(Task<AuthResult> task) {
        try {
            throw task.getException();
        } catch (FirebaseAuthInvalidCredentialsException malformedEmail) {
            mTextInputLayoutEmail.setError("Invalid Email");
            showProgressBar(false);
        } catch (FirebaseAuthUserCollisionException existEmail) {
            mTextInputLayoutEmail.setError("Email already in use");
            showProgressBar(false);
        } catch (Exception e) {
            showProgressBar(false);
            loginErrorDialog();
        }
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
    public void dismissDialogError(View view) {
        mDialogError.dismiss();
    }

    private void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.act_SUWEmail_CL_Root).setClickable(false);
        } else {
            mProgressBar.setVisibility(View.GONE);
            findViewById(R.id.act_SUWEmail_CL_Root).setClickable(true);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(SUWEmailFail);
    }
}
