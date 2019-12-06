package com.davidbuzatu.schedly.fragment;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.SettingsActivity;
import com.davidbuzatu.schedly.model.LogOut;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class ChangeEmailFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private TextInputLayout mTextInputLayoutEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_email, container, false);
        return mInflater;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_email_bar_title));
    }

    @Override
    public void onStart() {
        super.onStart();

        mTextInputLayoutEmail = mInflater.findViewById(R.id.frag_CEmail_TIL_email);
        setUpButtonSave();
    }

    private void setUpButtonSave() {
        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CEmail_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText emailEditText = mInflater.findViewById(R.id.frag_CEmail_TIET_email);
                String _email = emailEditText.getText().toString();
                if (validEmail(_email)) {
                    saveNewEmailInDatabase(_email);
                }
            }
        });
    }

    private boolean validEmail(String email) {
        if (email.isEmpty()) {
            mTextInputLayoutEmail.setError("Field required!");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mTextInputLayoutEmail.setError("Invalid Email");
            return false;
        }
        return true;
    }

    private void saveNewEmailInDatabase(String newEmail) {
        FirebaseUser _user = FirebaseAuth.getInstance().getCurrentUser();
        _user.updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    LogOut _logOut = new LogOut(mActivity);
                    _logOut.LogOutFromApp(true);
                } else {
                    throwExceptions(task);
                }
            }
        });

    }

    private void throwExceptions(Task<Void> task) {
        try {
            throw task.getException();
        } catch (FirebaseAuthInvalidCredentialsException malformedEmail) {
            mTextInputLayoutEmail.setError("Invalid Email");
        } catch (FirebaseAuthUserCollisionException existEmail) {
            mTextInputLayoutEmail.setError("Email already in use");
        } catch (Exception e) {
            Toast.makeText(mActivity,  "Please check your internet connection", Toast.LENGTH_LONG).show();
        }
    }

}
