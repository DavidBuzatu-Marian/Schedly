package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.MainActivity.SUWEmailFail;
import static com.example.schedly.MainActivity.SUWEmailSuccess;

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
                            FirebaseUser _user = mAuth.getCurrentUser();
                            addUserDataDatabase(_user);
                        } else {
                            throwExceptions(task);
                        }
                    }
                });
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
        }
    }

    private void addUserDataDatabase(final FirebaseUser user) {
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", mCCP.getFullNumberWithPlus());
        userToAdd.put("profession", null);
        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .set(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        addUserWorkingDaysInDB(user);
                    }
                });
    }

    private void addUserWorkingDaysInDB(FirebaseUser user) {
        Map<String, Object> daysOfTheWeek = getInitMap();
        FirebaseFirestore.getInstance().collection("workingDays")
                .document(user.getUid())
                .set(daysOfTheWeek)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        setResult(SUWEmailSuccess);
                        SignUpWithEmailActivity.this.finish();
                    }
                });
    }

    private Map<String, Object> getInitMap() {
        Map<String, Object> _data = new HashMap<>();
        _data.put("MondayStart", null);
        _data.put("MondayEnd", null);
        _data.put("TuesdayStart", null);
        _data.put("TuesdayEnd", null);
        _data.put("WednesdayStart", null);
        _data.put("WednesdayEnd", null);
        _data.put("ThursdayStart", null);
        _data.put("ThursdayEnd", null);
        _data.put("FridayStart", null);
        _data.put("FridayEnd", null);
        _data.put("SaturdayStart", null);
        _data.put("SaturdayEnd", null);
        _data.put("SundayStart", null);
        _data.put("SundayEnd", null);
        return _data;
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
