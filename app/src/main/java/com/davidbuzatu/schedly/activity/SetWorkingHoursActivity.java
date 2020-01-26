package com.davidbuzatu.schedly.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.AnimationTransitionOnActivity;
import com.davidbuzatu.schedly.model.User;
import com.davidbuzatu.schedly.packet_classes.PacketCardView;
import com.davidbuzatu.schedly.packet_classes.PacketSpinnerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private ArrayAdapter<CharSequence> mAdapterHours;
    private PacketSpinnerView mPacketSpinnerView;
    private PacketCardView mPacketCardView;
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

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
                    User.getInstance().setUserWorkingHours(mPacketSpinnerView.getDaysToAdd())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startScheduleDuration();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToastError();
                                }
                            });
                } else {
                    Toast.makeText(SetWorkingHoursActivity.this, "Both starting and ending hours are required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setUpAdapterAndPacket() {
        mAdapterHours = ArrayAdapter.createFromResource(this, R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPacketCardView = new PacketCardView(this, SetWorkingHoursActivity.this);
        mPacketSpinnerView = new PacketSpinnerView(this, mPacketCardView, SetWorkingHoursActivity.this);
        mPacketSpinnerView.setUpSpinners(mAdapterHours);
    }


    public void startScheduleDuration() {
        Intent _scheduleDurationIntent = new Intent(SetWorkingHoursActivity.this, ScheduleDurationActivity.class);
        startActivity(_scheduleDurationIntent);
        this.finish();
    }

    public void showToastError() {
        Toast.makeText(this, "Something went wrong! Please check your internet connection!", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
    }
}
