package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

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

public class SetPhoneNumberActivity extends AppCompatActivity {

    private String userID;
    private FirebaseFirestore mFireStore;
    private FirebaseAuth mAuth;
    private String TAG = "Database";
    private CountryCodePicker ccp;
    private EditText editTextCarrierNumber;
    private final int CA_CANCEL = 2003;

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

        Log.d("ID", userID);

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SPNumber_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write phone number to database
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                add_userData_to_Database(user);
            }
        });
    }


    private void add_userData_to_Database(FirebaseUser user) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("phoneNumber", ccp.getFullNumberWithPlus());
        userToAdd.put("profession", null);
        mFireStore.collection("users")
                .document(user.getUid())
                .set(userToAdd, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
        Intent calendarIntent = new Intent(SetPhoneNumberActivity.this, CalendarActivity.class);
        calendarIntent.putExtra("userID", user.getUid());
        startActivityForResult(calendarIntent, CA_CANCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == CA_CANCEL) {
            this.finish();
            System.exit(0);
        }
    }

}
