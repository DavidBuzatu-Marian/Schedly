package com.example.schedly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;

import com.example.schedly.fragment.SettingsFragment;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    private Toolbar mToolBar;
    private String mUserID;
    private String mUserAppointmentDuration;
    private HashMap<String, String> mWorkingHours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
        mToolBar = findViewById(R.id.frag_Settings_Toolbar);
        initSettingsFrament();
        setToolBarNavClick();

        Bundle _extras = getIntent().getExtras();
        getExtrasValues(_extras);
    }

    private void getExtrasValues(Bundle extras) {
        if (extras != null) {
            mUserID = extras.getString("userID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
            mWorkingHours = (HashMap<String, String>) extras.getSerializable("userWorkingHours");
        }
    }

    private void initSettingsFrament() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frag_Settings_FL_Holder, new SettingsFragment())
                .commit();
    }

    private void setToolBarNavClick() {
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

    public HashMap<String, String> getmWorkingHours() {
        return mWorkingHours;
    }
}
