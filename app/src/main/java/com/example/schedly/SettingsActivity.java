package com.example.schedly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;

import com.example.schedly.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {
    private Toolbar mToolBar;
    private String mUserID;
    private String mUserAppointmentDuration;
    private String mUserWorkingDaysID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
        mToolBar = findViewById(R.id.frag_Settings_Toolbar);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frag_Settings_FL_Holder, new SettingsFragment())
                .commit();

        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                }
                else {
                    finish();
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUserID = extras.getString("userID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
            mUserWorkingDaysID = extras.getString("userWorkingDaysID");
        }
    }


    public void setActionBarTitle(String _title) {
        mToolBar.setTitle(_title);
    }

    public String getmUserID() {
        return mUserID;
    }

    public String getmUserAppointmentDuration() {
        return mUserAppointmentDuration;
    }

    public String getmUserWorkingDaysID() {
        return mUserWorkingDaysID;
    }
}
