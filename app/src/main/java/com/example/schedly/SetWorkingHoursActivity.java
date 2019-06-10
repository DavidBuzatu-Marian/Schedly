package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private String userID;
    private String TAG = "RES";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SWHours_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write profession to database
                addUserDataToDatabase(userID);
            }
        });
    }

    private void addUserDataToDatabase(String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> daysToAdd = new HashMap<>();
        daysToAdd.put("profession", selectedProfessionName);
        mFireStore.collection("users")
                .document(userID)
                .update(userToAdd)
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
        Intent calendarIntent = new Intent(SetWorkingHoursActivity.this, CalendarActivity.class);
        calendarIntent.putExtra("userID", userID);
        startActivityForResult(calendarIntent, SWH_CANCEL);
    }
}
