package com.example.schedly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class SetProffesionActivity extends AppCompatActivity {

    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_proffesion);
        Bundle extras = getIntent().getExtras();


        if(extras != null) {
            userID = extras.getString("userID");
        }
        Log.d("ID", userID);
    }
}
