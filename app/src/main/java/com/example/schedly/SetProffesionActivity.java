package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.schedly.model.AnimationTransitionOnActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SetProffesionActivity extends AppCompatActivity implements View.OnClickListener{

    private final int CA_CANCEL = 2005;
    private String userID;
    private final int SWH_CANCEL = 2003;
    private String TAG = "RES";
    private int selectedProfession;
    private String selectedProfessionName;
    AnimationTransitionOnActivity _animationTransitionOnActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_proffesion);
        Bundle extras = getIntent().getExtras();


        if(extras != null) {
            userID = extras.getString("userID");
        }
        Log.d("ID", userID);
        /* set buttons on click listeners */
        Button dentistButton = findViewById(R.id.act_SProfession_BUT_Dentist);
        dentistButton.setOnClickListener(this);
        Button hairstylistButton = findViewById(R.id.act_SProfession_BUT_Hairstylist);
        hairstylistButton.setOnClickListener(this);
        Button freelancerButton = findViewById(R.id.act_SProfession_BUT_Freelancer);
        freelancerButton.setOnClickListener(this);
        Button housesitterButton = findViewById(R.id.act_SProfession_BUT_HouseSitter);
        housesitterButton.setOnClickListener(this);
        Button personaltrainerButton = findViewById(R.id.act_SProfession_BUT_PersonalTrainer);
        personaltrainerButton.setOnClickListener(this);
        Button otherButton = findViewById(R.id.act_SProfession_BUT_Other);
        otherButton.setOnClickListener(this);

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SProfession_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write profession to database
                if(selectedProfessionName != null) {
                    _animationTransitionOnActivity = new AnimationTransitionOnActivity(findViewById(R.id.act_SProfession_V_AnimationFill), (int) view.getX(), (int) view.getY());
                    addUserDataToDatabase(userID);
                }
                else {
                    Toast.makeText(SetProffesionActivity.this, "A profession is required!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addUserDataToDatabase(final String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> userToAdd = new HashMap<>();
        userToAdd.put("profession", selectedProfessionName);
        mFireStore.collection("users")
                .document(userID)
                .update(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startSetWorkingHours(userID);
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

    private void startSetWorkingHours(String userID) {
        Intent workingHoursIntent = new Intent(SetProffesionActivity.this, SetWorkingHoursActivity.class);
        workingHoursIntent.putExtra("userID", userID);
        startActivityForResult(workingHoursIntent, SWH_CANCEL);
    }

    @Override
    public void onClick(View v) {
        selectedProfession = v.getId();
        Log.d("Select", v.toString());
        switch (selectedProfession) {
            case R.id.act_SProfession_BUT_Dentist:
                selectedProfessionName = "Dentist";
                break;
            case R.id.act_SProfession_BUT_Freelancer:
                selectedProfessionName = "Freelancer";
                break;
            case R.id.act_SProfession_BUT_Hairstylist:
                selectedProfessionName = "Hairstylist";
                break;
            case R.id.act_SProfession_BUT_HouseSitter:
                selectedProfessionName = "HouseSitter";
                break;
            case R.id.act_SProfession_BUT_PersonalTrainer:
                selectedProfessionName = "PersonalTrainer";
                break;
            default:
                selectedProfessionName = "Other";
                selectedProfession = R.id.act_SProfession_BUT_Other;
                break;
        }
        setSelectedButton(selectedProfession);
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
        Log.d("Code", "" + requestCode + "");
        switch (resultCode) {
            case CA_CANCEL:
                setResult(CA_CANCEL);
                this.finish();
                break;
        }

    }
}
