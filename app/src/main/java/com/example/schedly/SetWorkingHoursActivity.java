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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.schedly.CalendarActivity.LOG_OUT;
import static com.example.schedly.MainActivity.CA_CANCEL;
import static com.example.schedly.MainActivity.SD_CANCEL;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private String mUserID;
    private String TAG = "WorkingHours";
    private ArrayAdapter<CharSequence> mAdapterHours;
    private PacketSpinnerView mPacketSpinnerView;
    private PacketCardView mPacketCardView;
    private String mUserPhoneNumber;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mUserPhoneNumber = extras.getString("userPhoneNumber");
            mUserID = extras.getString("userID");
        }

        setUpFloatingButton();
        setUpAdapterAndPacket();
    }

    private void setUpFloatingButton() {
        FloatingActionButton floatingActionButton = findViewById(R.id.act_SWHours_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean emptySpinner = mPacketSpinnerView.checkEmptySpinners();
                if (!emptySpinner) {
                    mAnimationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SWHours_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addUserDataToDatabase();
                } else {
                    Toast.makeText(SetWorkingHoursActivity.this, "Both starting and ending hours are required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setUpAdapterAndPacket() {
        mAdapterHours = ArrayAdapter.createFromResource(this,R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPacketCardView = new PacketCardView(this, SetWorkingHoursActivity.this);
        mPacketSpinnerView = new PacketSpinnerView(this, mPacketCardView, SetWorkingHoursActivity.this);
        mPacketSpinnerView.setUpSpinners(mAdapterHours);
    }

    private void addUserDataToDatabase() {
        Map<String, Object> daysToAdd = mPacketSpinnerView.getDaysToAdd();
        mWorkingHours =  mPacketSpinnerView.getWorkingDays();
        FirebaseFirestore.getInstance().collection("workingDays")
                .document(mUserID)
                .set(daysToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startScheduleDuration();
                    }
                });

    }

    private void startScheduleDuration() {
        Intent _scheduleHoursIntent = new Intent(SetWorkingHoursActivity.this, ScheduleDurationActivity.class);
        _scheduleHoursIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
        _scheduleHoursIntent.putExtra("userWorkingHours", mWorkingHours);
        _scheduleHoursIntent.putExtra("userID", mUserID);
        startActivityForResult(_scheduleHoursIntent, SD_CANCEL);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == CA_CANCEL || resultCode == LOG_OUT) {
            setResult(resultCode);
            this.finish();
        }
    }
}
