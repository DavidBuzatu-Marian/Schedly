package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.schedly.model.AnimationTransitionOnActivity;
import com.example.schedly.packet_classes.PacketCardView;
import com.example.schedly.packet_classes.PacketSpinnerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;
import static com.example.schedly.MainActivity.SD_CANCEL;
import static com.example.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private String userID;
    private String TAG = "RES";
    private String workingDaysID;
    private ArrayAdapter<CharSequence> mAdapterHours;

    private PacketSpinnerView mPacketSpinnerView;
    private PacketCardView mPacketCardView;

    private String mUserPhoneNumber;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mUserPhoneNumber = extras.getString("userPhoneNumber");
            workingDaysID = extras.getString("userWorkingDays");
            userID = extras.getString("userID");
            if(workingDaysID == null) {
                getUserWorkingDaysID(userID);
            }
        }

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SWHours_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write working hours to database
                boolean emptySpinner = mPacketSpinnerView.checkEmptySpinners();

                if (!emptySpinner) {
                    _animationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SWHours_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addUserDataToDatabase(userID);
                } else {
                    Toast.makeText(SetWorkingHoursActivity.this, "Both starting and ending hours are required!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mAdapterHours = ArrayAdapter.createFromResource(this,R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mPacketCardView = new PacketCardView(this, SetWorkingHoursActivity.this);

        mPacketSpinnerView = new PacketSpinnerView(this, mPacketCardView, SetWorkingHoursActivity.this);
        mPacketSpinnerView.setUpSpinners(mAdapterHours);

    }

    private void getUserWorkingDaysID(String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        mFireStore.collection("users")
                .document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        workingDaysID = document.get("workingDaysID").toString();
                        Log.d("IDwork", workingDaysID);
                    }
                });
    }

    private void addUserDataToDatabase(final String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> daysToAdd = mPacketSpinnerView.getDaysToAdd();
        mWorkingHours =  mPacketSpinnerView.getWorkingDays();

        mFireStore.collection("workingDays")
                .document(workingDaysID)
                .update(daysToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startCalendar(userID);
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

    private void startCalendar(String userID) {
        Intent scheduleHoursIntent = new Intent(SetWorkingHoursActivity.this, ScheduleDurationActivity.class);
        scheduleHoursIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
        scheduleHoursIntent.putExtra("userWorkingDaysID", workingDaysID);
        scheduleHoursIntent.putExtra("userWorkingHours", mWorkingHours);
        scheduleHoursIntent.putExtra("userID", userID);
        startActivityForResult(scheduleHoursIntent, SD_CANCEL);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("request", requestCode + "");
        switch (resultCode) {
            case LOG_OUT:
                setResult(LOG_OUT);
                this.finish();
                break;
            case CA_CANCEL:
                setResult(CA_CANCEL);
                this.finish();
                break;
            case EMAIL_CHANGED:
                setResult(EMAIL_CHANGED);
                finish();
                break;
            case PASSWORD_CHANGED:
                setResult(PASSWORD_CHANGED);
                finish();
                break;
            case WORKING_HOURS_CHANGED:
                setResult(WORKING_HOURS_CHANGED);
                finish();
                break;
        }
    }
}
