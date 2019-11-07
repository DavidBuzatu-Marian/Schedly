package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.schedly.MainActivity.PR_SUCCESS;

public class ForgotPasswordActivity extends AppCompatActivity {


    private final int PR_FAIL = 4011;
    private ProgressBar mProgressBar;
    private TextInputLayout mTextInputLayoutEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mProgressBar = findViewById(R.id.act_FPassword_PB);
        mTextInputLayoutEmail = findViewById(R.id.act_FPassword_TIL_email);
        if (getIntent().hasExtra("Email")) {
            TextInputEditText _txtInputEmail = findViewById(R.id.act_FPassword_TIET_email);
            _txtInputEmail.setText(getIntent().getStringExtra("Email"));
        }
        setUpResetButton();
    }

    private void setUpResetButton() {
        final Button _buttonReset = findViewById(R.id.act_FPassword_Send);
        _buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView EMAIL = findViewById(R.id.act_FPassword_TIET_email);
                if (!EMAIL.getText().toString().isEmpty()) {
                    showProgressBar(true);
                    resetPassword(EMAIL.getText().toString());
                } else {
                    mTextInputLayoutEmail.setError("Email is required!");
                }
            }
        });
    }

    private void resetPassword(String _email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(_email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            setResult(PR_SUCCESS);
                            ForgotPasswordActivity.this.finish();
                        } else {
                            mTextInputLayoutEmail.setError("Email is not registered!");
                            showProgressBar(false);
                        }
                    }
                });
    }

    private void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.act_FPassword_RL_Root).setClickable(false);
        } else {
            mProgressBar.setVisibility(View.GONE);
            findViewById(R.id.act_FPassword_RL_Root).setClickable(true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(PR_FAIL);
    }
}
