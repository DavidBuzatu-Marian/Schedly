package com.davidbuzatu.schedly.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.fragment.SettingsFragment;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    private Toolbar mToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
        mToolBar = findViewById(R.id.frag_Settings_Toolbar);
        initSettingsFrament();
        setToolBarNavClick();
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
}
