package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.schedly.model.AnimationTransitionOnActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.SP_CANCEL;

public class SetPhoneNumberActivity extends AppCompatActivity {

    private String mUserID;
    private String TAG = "Database";
    private CountryCodePicker mCCP;
    private EditText mEditTextCarrierNumber;
    private boolean mValidNumber = false;
    private String mPhoneNumberReturn;
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_phone_number);
        Bundle _extras = getIntent().getExtras();
        if (_extras != null) {
            mUserID = _extras.getString("userID");
        }
        mCCP = findViewById(R.id.act_SPNumber_cpp);
        mEditTextCarrierNumber = findViewById(R.id.act_SPNumber_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mCCP.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });

        FloatingActionButton _floatingActionButton = findViewById(R.id.act_SPNumber_floating_action_button);
        _floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write phone number to database if valid
                if(mValidNumber) {
                    _animationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SPNumber_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addUserDataToDatabase(mUserID);
                }
                else {
                    Toast.makeText(SetPhoneNumberActivity.this, "Phone number is invalid!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void addUserDataToDatabase(final String userID) {
        FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
        mPhoneNumberReturn = mEditTextCarrierNumber.getText().toString();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", mCCP.getFullNumberWithPlus());
        userToAdd.put("profession", null);
        _firebaseFirestore.collection("users")
                .document(userID)
                .set(userToAdd, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startSetProfessionActivity(userID);
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    private void startSetProfessionActivity(String userID) {
        Intent _setProfessionIntent = new Intent(SetPhoneNumberActivity.this, SetProfessionActivity.class);
        _setProfessionIntent.putExtra("userID", userID);
        startActivityForResult(_setProfessionIntent, SP_CANCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == SP_CANCEL) {
            mEditTextCarrierNumber.setText(mPhoneNumberReturn);
        }
        else if(resultCode == CA_CANCEL) {
            setResult(CA_CANCEL);
            this.finish();
        }
    }

}
