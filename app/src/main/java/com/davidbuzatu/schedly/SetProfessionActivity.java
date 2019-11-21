package com.davidbuzatu.schedly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.davidbuzatu.schedly.model.AnimationTransitionOnActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.davidbuzatu.schedly.CalendarActivity.LOG_OUT;
import static com.davidbuzatu.schedly.MainActivity.CA_CANCEL;
import static com.davidbuzatu.schedly.MainActivity.SWH_CANCEL;

public class SetProfessionActivity extends AppCompatActivity implements View.OnClickListener {
    private String userID;
    private String TAG = "SetProfession";
    private int mSelectedProfession;
    private String mSelectedProfessionName;
    private String mUserPhoneNumber;
    AnimationTransitionOnActivity mAnimationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_proffesion);
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            ;
            mUserPhoneNumber = extras.getString("userPhoneNumber");
            userID = extras.getString("userID");
        }
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
                    addUserDataToDatabase(userID);
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

    private void addUserDataToDatabase(final String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("profession", mSelectedProfessionName);
        mFireStore.collection("users")
                .document(userID)
                .update(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startSetWorkingHours(userID);
                    }
                });
    }

    private void startSetWorkingHours(String userID) {
        Intent _workingHoursIntent = new Intent(SetProfessionActivity.this, SetWorkingHoursActivity.class);
        _workingHoursIntent.putExtra("userID", userID);
        _workingHoursIntent.putExtra("userPhoneNumber", mUserPhoneNumber);
        startActivityForResult(_workingHoursIntent, SWH_CANCEL);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == CA_CANCEL || resultCode == LOG_OUT) {
            setResult(resultCode);
            this.finish();
        }
    }
}
