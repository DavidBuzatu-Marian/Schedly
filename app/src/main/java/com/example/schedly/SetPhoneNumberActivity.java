package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.schedly.model.AnimationTransitionOnActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.SP_CANCEL;

public class SetPhoneNumberActivity extends AppCompatActivity {

    private String userID;
    private String TAG = "Database";
    private CountryCodePicker ccp;
    private EditText editTextCarrierNumber;
    private boolean mValidNumber = false;
    private String phoneNumberReturn;
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_phone_number);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userID = extras.getString("userID");
        }
        ccp = findViewById(R.id.act_SPNumber_cpp);
        editTextCarrierNumber = findViewById(R.id.act_SPNumber_ET_carrierNumber);
        ccp.registerCarrierNumberEditText(editTextCarrierNumber);

        ccp.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SPNumber_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write phone number to database if valid
                if(mValidNumber) {
                    _animationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SPNumber_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addUserDataToDatabase(userID);
                }
                else {
                    Toast.makeText(SetPhoneNumberActivity.this, "Phone number is invalid!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void addUserDataToDatabase(final String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        phoneNumberReturn = editTextCarrierNumber.getText().toString();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", ccp.getFullNumberWithPlus());
        userToAdd.put("profession", null);
        mFireStore.collection("users")
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
        Intent setPhoneNumberIntent = new Intent(SetPhoneNumberActivity.this, SetProffesionActivity.class);
        setPhoneNumberIntent.putExtra("userID", userID);
        startActivityForResult(setPhoneNumberIntent, SP_CANCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == SP_CANCEL) {
            editTextCarrierNumber.setText(phoneNumberReturn);
        }
        else if(resultCode == CA_CANCEL) {
            setResult(CA_CANCEL);
            this.finish();
        }
    }

}
