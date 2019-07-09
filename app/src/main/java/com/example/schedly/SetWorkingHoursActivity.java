package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.midi.MidiDevice;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.schedly.model.AnimationTransitionOnActivity;
import com.example.schedly.packet_classes.PacketLinearLayout;
import com.example.schedly.packet_classes.PacketSpinnerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private String userID;
    private String TAG = "RES";
    private final int CA_CANCEL = 2005;
    private final int SD_CANCEL = 2004;
    private String workingDaysID;
    private ArrayAdapter<CharSequence> mAdapterHours;

    private PacketSpinnerView mPacketSpinnerView;
    private PacketLinearLayout mLinearLayout;
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
            getUserWorkingDaysID(userID);
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

        mLinearLayout = new PacketLinearLayout(SetWorkingHoursActivity.this, SetWorkingHoursActivity.this);

        mPacketSpinnerView = new PacketSpinnerView(this, mLinearLayout, SetWorkingHoursActivity.this);
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
        Intent calendarIntent = new Intent(SetWorkingHoursActivity.this, ScheduleDurationActivity.class);
        calendarIntent.putExtra("userID", userID);
        startActivityForResult(calendarIntent, SD_CANCEL);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("request", requestCode + "");
        if (resultCode == CA_CANCEL) {
            setResult(CA_CANCEL);
            this.finish();
        }
    }
}
