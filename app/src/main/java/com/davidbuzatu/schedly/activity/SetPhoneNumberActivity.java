package com.davidbuzatu.schedly.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.AnimationTransitionOnActivity;
import com.davidbuzatu.schedly.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hbb20.CountryCodePicker;


public class SetPhoneNumberActivity extends AppCompatActivity {

    private String mUserID;
    private String TAG = "SetPhoneNumber";
    private CountryCodePicker mCCP;
    private EditText mEditTextCarrierNumber;
    private boolean mValidNumber = false;
    private String mPhoneNumberReturn;
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_phone_number);
        Bundle _extras = getIntent().getExtras();
        if (_extras != null) {
            mUserID = _extras.getString("userID");
        }
        setCCP();
        setUpFloatingButton();
    }

    private void setUpFloatingButton() {
        FloatingActionButton _floatingActionButton = findViewById(R.id.act_SPNumber_floating_action_button);
        _floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mValidNumber) {
                    mAnimationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SPNumber_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    User.getInstance().setUserPhoneNumber(mCCP.getFullNumberWithPlus())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startSetProfessionActivity();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToastError();
                                }
                            });
                } else {
                    Toast.makeText(SetPhoneNumberActivity.this, "Phone number is invalid!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setCCP() {
        mCCP = findViewById(R.id.act_SPNumber_cpp);
        mEditTextCarrierNumber = findViewById(R.id.act_SPNumber_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mCCP.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });
    }


//    private void addUserDataToDatabase(final String userID) {
//        mPhoneNumberReturn = mEditTextCarrierNumber.getText().toString();
//        Map<String, Object> userToAdd = new HashMap<>();
//        userToAdd.put("phoneNumber", mCCP.getFullNumberWithPlus());
//        userToAdd.put("profession", null);
//        FirebaseFirestore.getInstance().collection("users")
//                .document(userID)
//                .set(userToAdd, SetOptions.merge())
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        startSetProfessionActivity(userID);
//                    }
//                });
//    }

    public void startSetProfessionActivity() {
        Intent _setProfessionIntent = new Intent(SetPhoneNumberActivity.this, SetProfessionActivity.class);
        startActivity(_setProfessionIntent);
        this.finish();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
    }

    public void showToastError() {
        Toast.makeText(this, "Something went wrong! Please check your internet connection!", Toast.LENGTH_LONG).show();
    }
}
