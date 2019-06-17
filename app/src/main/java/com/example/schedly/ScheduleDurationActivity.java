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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ScheduleDurationActivity extends AppCompatActivity {

    private String userID;
    private final String TAG = "SDuration";
    private final int CA_CANCEL = 2005;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_duration);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }

        final EditText _sDurationTV = findViewById(R.id.act_SDuration_TIET_MinutesSelector);

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SDuration_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addDurationToDB(_sDurationTV.getText().toString());
            }
        });
    }

    private void addDurationToDB(String minutes) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> duration = new HashMap<>();
        duration.put("appointmentsDuration", minutes);
        db.collection("users")
                .document(userID)
                .update(duration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        startCalendar(userID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    private void startCalendar(String userID) {
        Intent calendarIntent = new Intent(ScheduleDurationActivity.this, CalendarActivity.class);
        calendarIntent.putExtra("userID", userID);
        startActivityForResult(calendarIntent, CA_CANCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CA_CANCEL) {
            setResult(CA_CANCEL);
            this.finish();
        }
    }
}
