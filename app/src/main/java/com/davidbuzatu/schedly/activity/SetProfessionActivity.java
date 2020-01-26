package com.davidbuzatu.schedly.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.AnimationTransitionOnActivity;
import com.davidbuzatu.schedly.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class SetProfessionActivity extends AppCompatActivity implements View.OnClickListener {
    private int mSelectedProfession;
    private String mSelectedProfessionName;
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_proffesion);
        /* set buttons on click listeners */
        setButtonsClick();
        setUpFloatingButton();
    }

    private void setUpFloatingButton() {
        FloatingActionButton floatingActionButton = findViewById(R.id.act_SProfession_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write profession to database
                if (mSelectedProfessionName != null) {
                    mAnimationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SProfession_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    User.getInstance().setUserProfession(mSelectedProfessionName)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startSetWorkingHours();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToastError();
                                }
                            });
                } else {
                    Toast.makeText(SetProfessionActivity.this, "A profession is required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setButtonsClick() {
        Button _dentistButton = findViewById(R.id.act_SProfession_BUT_Dentist);
        _dentistButton.setOnClickListener(this);
        Button _hairstylistButton = findViewById(R.id.act_SProfession_BUT_Hairstylist);
        _hairstylistButton.setOnClickListener(this);
        Button _freelancerButton = findViewById(R.id.act_SProfession_BUT_Freelancer);
        _freelancerButton.setOnClickListener(this);
        Button _houseSitterButton = findViewById(R.id.act_SProfession_BUT_HouseSitter);
        _houseSitterButton.setOnClickListener(this);
        Button _personalTrainerButton = findViewById(R.id.act_SProfession_BUT_PersonalTrainer);
        _personalTrainerButton.setOnClickListener(this);
        Button _otherButton = findViewById(R.id.act_SProfession_BUT_Other);
        _otherButton.setOnClickListener(this);
    }

    public void startSetWorkingHours() {
        Intent _workingHoursIntent = new Intent(SetProfessionActivity.this, SetWorkingHoursActivity.class);
        startActivity(_workingHoursIntent);
        this.finish();
    }

    @Override
    public void onClick(View v) {
        mSelectedProfession = v.getId();
        switch (mSelectedProfession) {
            case R.id.act_SProfession_BUT_Dentist:
                mSelectedProfessionName = "Dentist";
                break;
            case R.id.act_SProfession_BUT_Freelancer:
                mSelectedProfessionName = "Freelancer";
                break;
            case R.id.act_SProfession_BUT_Hairstylist:
                mSelectedProfessionName = "Hairstylist";
                break;
            case R.id.act_SProfession_BUT_HouseSitter:
                mSelectedProfessionName = "HouseSitter";
                break;
            case R.id.act_SProfession_BUT_PersonalTrainer:
                mSelectedProfessionName = "PersonalTrainer";
                break;
            default:
                mSelectedProfessionName = "Other";
                mSelectedProfession = R.id.act_SProfession_BUT_Other;
                break;
        }
        setSelectedButton(mSelectedProfession);
    }

    private void setSelectedButton(int selectedProfession) {
        Button currentButton;
        findViewById(R.id.act_SProfession_BUT_Dentist).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        findViewById(R.id.act_SProfession_BUT_PersonalTrainer).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        findViewById(R.id.act_SProfession_BUT_HouseSitter).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        findViewById(R.id.act_SProfession_BUT_Freelancer).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        findViewById(R.id.act_SProfession_BUT_Hairstylist).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        findViewById(R.id.act_SProfession_BUT_Other).setBackground(ContextCompat.getDrawable(this, R.drawable.button_profession));
        currentButton = findViewById(selectedProfession);
        currentButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_selected_profession));
    }

    public void showToastError() {
        Toast.makeText(this, "Please select a profession and check your internet connection!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
    }
}
