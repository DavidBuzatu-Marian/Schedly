package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout mTextInputLayoutEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);


        mTextInputLayoutEmail = findViewById(R.id.act_FPassword_TIL_email);
        final Button BUTTON_RESET = findViewById(R.id.act_FPassword_Send);
        BUTTON_RESET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView EMAIL = (TextView) findViewById(R.id.act_FPassword_TIET_email);
                if(!EMAIL.getText().toString().isEmpty()) {
                    resetPassword(EMAIL.getText().toString());
                }
            }
        });
    }

    private void resetPassword(String _email) {
        FirebaseAuth _firebaseAuth = FirebaseAuth.getInstance();
        _firebaseAuth.sendPasswordResetEmail(_email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Log.d("PASSWORD", "Email sent!");
                        }
                        else {
                            mTextInputLayoutEmail.setError("Email is not registered!");
                        }
                    }
                });
    }
}
