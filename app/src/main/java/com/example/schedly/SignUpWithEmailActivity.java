package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

public class SignUpWithEmailActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private final String TAG = "RES";
    /* country getter */
    private CountryCodePicker ccp;
    private EditText editTextCarrierNumber;

    @Override
    public void onStart() {
        super.onStart();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_with_email);

        ccp = findViewById(R.id.ccp);
        editTextCarrierNumber = findViewById(R.id.act_SUWEmail_ET_carrierNumber);
        ccp.registerCarrierNumberEditText(editTextCarrierNumber);

        mAuth = FirebaseAuth.getInstance();

        final Button buttonSignUp = findViewById(R.id.act_signup_BUT_signup);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("number", ccp.getFullNumber());
                final EditText editTextEmail = findViewById(R.id.act_SUWEmail_TIET_email);
                final EditText editTextPass = findViewById(R.id.act_SUWEmail_TIET_password);
                if(inputValidation(editTextEmail.getText().toString(), editTextPass.getText().toString())) {
                    Log.d("Sign_up", "yes");
                    signUpWithEmailAndPassword(editTextEmail.getText().toString(), editTextPass.getText().toString());
                }
                else {
                    Log.d("Sign_up", "no");
                }
            }
        });
    }

    private boolean inputValidation(String email, String password) {
        TextInputLayout textInputLayoutEmail = findViewById(R.id.act_SUWEmail_TIL_email);
        TextInputLayout textInputLayoutPass = findViewById(R.id.act_SUWEmail_TIL_password);
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

                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpWithEmailActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            //updateUI(null);
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
    }
}
