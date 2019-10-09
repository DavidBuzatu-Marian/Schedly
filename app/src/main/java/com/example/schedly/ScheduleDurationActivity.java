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

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;
import static com.example.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class ScheduleDurationActivity extends AppCompatActivity {

    private String mUserID;
    private final String TAG = "SDuration";
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;
    private String mUserPhoneNumber, mUserAppointmentsDuration;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private String mUserDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_duration);

        Bundle _extras = getIntent().getExtras();
        if(_extras != null) {
            mUserPhoneNumber = _extras.getString("userPhoneNumber");
            mWorkingHours = (HashMap<String, String>) _extras.getSerializable("userWorkingHours");
            mUserID = _extras.getString("userID");
        }
        setUpFloatingButton();
    }

    private void setUpFloatingButton() {
        final EditText _sDurationTV = findViewById(R.id.act_SDuration_TIET_MinutesSelector);
        final EditText _dNameTV = findViewById(R.id.act_SDuration_TIET_DisplayName);
        FloatingActionButton _floatingActionButton = findViewById(R.id.act_SDuration_floating_action_button);
        _floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!errorDetected(_sDurationTV, _dNameTV)) {
                    mAnimationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SDuration_V_AnimationFill), (int) view.getX(), (int) view.getY());
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
        mUserAppointmentsDuration = minutes;
        mUserDisplayName = name;
        Map<String, Object> duration = new HashMap<>();
        duration.put("appointmentsDuration", minutes);
        duration.put("displayName", name);
        FirebaseFirestore.getInstance().collection("users")
                .document(mUserID)
                .update(duration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startCalendarActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    private void startCalendarActivity() {
        Intent _calendarIntent = new Intent(ScheduleDurationActivity.this, CalendarActivity.class);
        _calendarIntent.putExtra("userID", mUserID);
        _calendarIntent.putExtra("userWorkingHours", mWorkingHours);
        _calendarIntent.putExtra("userAppointmentDuration", mUserAppointmentsDuration);
        _calendarIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(_calendarIntent, CA_CANCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode);
        this.finish();
    }
}
