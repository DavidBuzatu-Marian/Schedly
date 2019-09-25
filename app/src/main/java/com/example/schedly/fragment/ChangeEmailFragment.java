package com.example.schedly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import static com.example.schedly.MainActivity.EMAIL_CHANGED;

public class ChangeEmailFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;
    private GoogleSignInClient mGoogleSignInClient;
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
        if(email.isEmpty()) {
            mTextInputLayoutEmail.setError("Field required!");
            return false;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
                if(task.isSuccessful()) {
                    logOut();
                }
                else {
                    try {
                        throw task.getException();
                    }
                    catch (FirebaseAuthInvalidCredentialsException malformedEmail)
                    {

                        mTextInputLayoutEmail.setError("Invalid Email");
                    }
                    catch (FirebaseAuthUserCollisionException existEmail)
                    {
                        mTextInputLayoutEmail.setError("Email already in use");
                    }
                    catch (Exception e)
                    {
                        Log.d("ErrorOnEmailChange", "onComplete: " + e.getMessage());
                    }
                }
            }
        });

    }

    private void logOut() {
        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        mGoogleSignInClient.signOut();
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        Intent stopServiceIntent = new Intent(mActivity, MonitorIncomingSMSService.class);
        stopServiceIntent.setAction("ACTION.STOPFOREGROUND_ACTION");
        getActivity().setResult(EMAIL_CHANGED);
        getActivity().finish();
    }
}
