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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

public class SetPhoneNumberActivity extends AppCompatActivity {

    private String userID;
    private FirebaseFirestore mFireStore;
    private String TAG = "Database";
    private CountryCodePicker ccp;
    private EditText editTextCarrierNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_phone_number);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }
        Log.d("ID", userID);

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SPNumber_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ccp = findViewById(R.id.act_SPNumber_cpp);
                editTextCarrierNumber = findViewById(R.id.act_SPNumber_ET_carrierNumber);
                ccp.registerCarrierNumberEditText(editTextCarrierNumber);
                // Write phone number to database
                mFireStore = FirebaseFirestore.getInstance();
                CollectionReference users = mFireStore.collection("users");
                users.add(userID);
            }
        });
    }
}
