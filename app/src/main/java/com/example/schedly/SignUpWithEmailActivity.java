package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpWithEmailActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private final String TAG = "RES";

    @Override
    public void onStart() {
        super.onStart();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_with_email);


        mAuth = FirebaseAuth.getInstance();

        final Button buttonSignUp = findViewById(R.id.act_signup_BUT_signup);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText editTextEmail = findViewById(R.id.act_signup_ET_email);
                final EditText editTextPass = findViewById(R.id.act_signup_ET_password);
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
        boolean hasDigit_TRUE = false;
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"empty fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        /* password validation */
        /* check if length is between limits */
        if(password.length() < 8 || password.length() > 20) {
            Toast.makeText(this,"length pass", Toast.LENGTH_SHORT).show();
            return false;
        }
        /* it has digits */
        for(char digit : password.toCharArray()) {
            if(Character.isDigit(digit)) {
                hasDigit_TRUE = true;
            }
        }
        if(!hasDigit_TRUE) {
            Toast.makeText(this,"no digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        /* it has uppercase/ lowercase letter */
        if(password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            Toast.makeText(this,"upper-lower", Toast.LENGTH_SHORT).show();
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
}
