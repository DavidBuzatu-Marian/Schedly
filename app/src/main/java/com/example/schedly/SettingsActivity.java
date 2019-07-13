package com.example.schedly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.schedly.fragments.SettingsFragment;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private Toolbar mToolBar;

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
    }


    public void setActionBarTitle(String _title) {
        mToolBar.setTitle(_title);
    }
}