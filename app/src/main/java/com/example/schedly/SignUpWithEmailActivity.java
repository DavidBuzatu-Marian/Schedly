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
    private final String TAG = "RESULT";
    /* country getter */
    private CountryCodePicker ccp;
    private final int SUWESuccess = 2000;
    private EditText editTextCarrierNumber;
    TextInputLayout textInputLayoutEmail;
    TextInputLayout textInputLayoutPass;
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

        ccp = findViewById(R.id.ccp);
        editTextCarrierNumber = findViewById(R.id.act_SUWEmail_ET_carrierNumber);
        ccp.registerCarrierNumberEditText(editTextCarrierNumber);

        mAuth = FirebaseAuth.getInstance();
        textInputLayoutEmail = findViewById(R.id.act_SUWEmail_TIL_email);
        textInputLayoutPass = findViewById(R.id.act_SUWEmail_TIL_password);

        if(getIntent().hasExtra("Email")) {
            TextInputEditText _txtInputEmail = findViewById(R.id.act_SUWEmail_TIET_email);
            _txtInputEmail.setText(getIntent().getStringExtra("Email"));
        }

        final Button buttonSignUp = findViewById(R.id.act_signup_BUT_signup);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText editTextEmail = findViewById(R.id.act_SUWEmail_TIET_email);
                final EditText editTextPass = findViewById(R.id.act_SUWEmail_TIET_password);
                if(inputValidation(editTextEmail.getText().toString(), editTextPass.getText().toString())) {
                    showProgressBar(true);
                    Log.d("RESULT", "OKAY");
                    signUpWithEmailAndPassword(editTextEmail.getText().toString(), editTextPass.getText().toString());
                }
            }
        });

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SUWEmail_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(SUWEmailFail);
                SignUpWithEmailActivity.this.finish();
            }
        });

        final ImageView imageViewShowPassword = findViewById(R.id.act_SUWEmail_IV_ShowPassword);
        imageViewShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextPass = findViewById(R.id.act_SUWEmail_TIET_password);
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
    }

    private boolean inputValidation(String email, String password) {
        boolean hasDigit_TRUE = false;
        /* email validation */
        if(email.isEmpty()) {
            textInputLayoutEmail.setError("Field required!");
            return false;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.setError("Invalid Email");
            return false;
        }
        textInputLayoutEmail.setErrorEnabled(false);
        /* password validation */
        if(password.isEmpty()) {
            textInputLayoutPass.setError("Field required!");
            return false;
        }
        /* check if length is between limits */
        if(password.length() < 8 || password.length() > 20) {
            textInputLayoutPass.setError("Password must be between 8 and 20 characters");
            return false;
        }
        /* it has digits */
        for(char digit : password.toCharArray()) {
            if(Character.isDigit(digit)) {
                hasDigit_TRUE = true;
            }
        }
        if(!hasDigit_TRUE) {
            textInputLayoutPass.setError("Password must have digits");
            return false;
        }
        /* it has uppercase/ lowercase letter */
        if(password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            textInputLayoutPass.setError("Password needs lowercase and uppercase letters");
            return false;
        }
        textInputLayoutPass.setErrorEnabled(false);

        /* number is not okay */
        if(!ccp.isValidFullNumber()) {
            Toast.makeText(this,"Invalid number", Toast.LENGTH_SHORT).show();
            return false;
        }
        /* validation is okay */
        return true;
    }

    private void signUpWithEmailAndPassword(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            add_userData_to_Database(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            try {
                                throw task.getException();
                            }
                            catch (FirebaseAuthInvalidCredentialsException malformedEmail)
                            {
                                Log.d(TAG, "onComplete: malformed_email");
                                textInputLayoutEmail.setError("Invalid Email");
                                showProgressBar(false);
                            }
                            catch (FirebaseAuthUserCollisionException existEmail)
                            {
                                Log.d(TAG, "onComplete: exist_email");
                                textInputLayoutEmail.setError("Email already in use");
                                showProgressBar(false);
                            }
                            catch (Exception e)
                            {
                                Log.d(TAG, "onComplete: " + e.getMessage());
                            }
                        }

                        // ...
                    }
                });
    }

    private void add_userData_to_Database(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", ccp.getFullNumberWithPlus());
        userToAdd.put("profession", null);
        db.collection("users")
                .document(user.getUid())
                .set(userToAdd);
        Log.d(TAG, "CREATED");
        setResult(SUWEmailSuccess);
        SignUpWithEmailActivity.this.finish();
    }
    private void showProgressBar(boolean show) {
        if(show) {
            mProgressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.act_SUWEmail_CL_Root).setClickable(false);
        }
        else {
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
