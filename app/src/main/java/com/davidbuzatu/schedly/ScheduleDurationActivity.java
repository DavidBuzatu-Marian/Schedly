package com.davidbuzatu.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.davidbuzatu.schedly.model.AnimationTransitionOnActivity;
import com.davidbuzatu.schedly.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.davidbuzatu.schedly.MainActivity.CA_CANCEL;

public class ScheduleDurationActivity extends AppCompatActivity {

    AnimationTransitionOnActivity mAnimationTransitionOnActivity;
    private boolean[] fetched = new boolean[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_duration);

        setUpFloatingButton();
    }

    private void setUpFloatingButton() {
        final EditText _sDurationTV = findViewById(R.id.act_SDuration_TIET_MinutesSelector);
        final EditText _dNameTV = findViewById(R.id.act_SDuration_TIET_DisplayName);
        FloatingActionButton _floatingActionButton = findViewById(R.id.act_SDuration_floating_action_button);
        _floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!errorDetected(_sDurationTV, _dNameTV)) {
                    mAnimationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SDuration_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    User.getInstance().setUserAppointmentDuration(_sDurationTV.getText().toString(), _dNameTV.getText().toString())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    updateUserInfo();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToastError();
                                }
                            });
                } else {
                    Toast.makeText(ScheduleDurationActivity.this, "A valid value is required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean errorDetected(EditText _sDurationTV, EditText _dNameTV) {
        if (_sDurationTV.getText().toString().equals("")) {
            _sDurationTV.setError("Please complete this field");
            return true;
        }
        if (_dNameTV.getText().toString().equals("")) {
            _dNameTV.setError("Please complete this field");
            return true;
        }
        return false;
    }

    public void updateUserInfo() {
        User.getInstance().getUserInfo(FirebaseAuth.getInstance().getCurrentUser()).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[0] = true;
                if (isFinishedFetching()) {
                    startCalendarActivity();
                }
            }
        });
        User.getInstance().getUserWorkingHoursFromDB(FirebaseAuth.getInstance().getCurrentUser()).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                fetched[1] = true;
                if (isFinishedFetching()) {
                    startCalendarActivity();
                }
            }
        });
    }


    public void startCalendarActivity() {
        Intent _calendarIntent = new Intent(ScheduleDurationActivity.this, CalendarActivity.class);
        _calendarIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(_calendarIntent);
        this.finish();
    }

    private boolean isFinishedFetching() {
        for (Boolean b : fetched) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode);
        this.finish();
    }

    public void showToastError() {
        Toast.makeText(this, "Something went wrong! Please check your internet connection!", Toast.LENGTH_LONG).show();
    }
}
