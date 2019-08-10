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

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.MainActivity.CA_CANCEL;

public class ScheduleDurationActivity extends AppCompatActivity {

    private String userID;
    private final String TAG = "SDuration";
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_duration);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }

        final EditText _sDurationTV = findViewById(R.id.act_SDuration_TIET_MinutesSelector);
        final EditText _dNameTV = findViewById(R.id.act_SDuration_TIET_DisplayName);

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SDuration_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!errorDetected(_sDurationTV, _dNameTV)) {
                    _animationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SDuration_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addDurationToDB(_sDurationTV.getText().toString(), _dNameTV.getText().toString());
                }
                else {
                    Toast.makeText(ScheduleDurationActivity.this, "A valid value is required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean errorDetected(EditText _sDurationTV, EditText _dNameTV) {
        if(_sDurationTV.getText().toString().equals("")) {
           _sDurationTV.setError("Please complete this field");
           return true;
        }
        if(_dNameTV.getText().toString().equals("")) {
            _dNameTV.setError("Please complete this field");
            return true;
        }
        return false;
    }

    private void addDurationToDB(String minutes, String name) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> duration = new HashMap<>();
        duration.put("appointmentsDuration", minutes);
        duration.put("displayName", name);
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
        calendarIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
