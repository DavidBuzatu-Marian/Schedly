package com.example.schedly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.schedly.MainActivity.PASSWORD_CHANGED;

public class ChangePasswordFragment extends Fragment implements View.OnClickListener {
    private FragmentActivity mActivity;
    private View mInflater;
    private GoogleSignInClient mGoogleSignInClient;
    private TextInputLayout mTextInputLayoutCurrentPassword,
                            mTextInputLayoutNewPassword,
                            mTextInputLayoutNewPasswordConfirm;
    private boolean mShowCurrentPasswordTrue = false,
                    mShowNewPasswordTrue = false,
                    mShowNewPasswordConfirmTrue = false;
    private EditText mCurrentPasswordEditText,
                    mNewPasswordEditText,
                    mNewPasswordConfirmEditText;
    private ImageView mCurrentPasswordShowImageView,
                    mNewPasswordShowImageView,
                    mNewPasswordConfirmShowImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_password, container, false);
        return mInflater;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Change password");
    }

    @Override
    public void onStart() {
        super.onStart();

        mCurrentPasswordShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowCurrentPassword);
        mCurrentPasswordShowImageView.setOnClickListener(this);
        mNewPasswordShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPassword);
        mNewPasswordShowImageView.setOnClickListener(this);
        mNewPasswordConfirmShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPasswordConfirm);
        mNewPasswordConfirmShowImageView.setOnClickListener(this);

        mTextInputLayoutCurrentPassword = mInflater.findViewById(R.id.frag_CPassword_TIL_CurrentPassword);
        mTextInputLayoutNewPassword = mInflater.findViewById(R.id.frag_CPassword_TIL_NewPassword);
        mTextInputLayoutNewPasswordConfirm = mInflater.findViewById(R.id.frag_CPassword_TIL_NewPasswordConfirm);

        mCurrentPasswordEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_CurrentPassword);
        mNewPasswordEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_NewPassword);
        mNewPasswordConfirmEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_NewPasswordConfirm);

        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CPassword_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] _passwords = new String[3];
                _passwords[0] = mCurrentPasswordEditText.getText().toString();
                _passwords[1] = mNewPasswordEditText.getText().toString();
                _passwords[2] = mNewPasswordConfirmEditText.getText().toString();
                if (validPasswords(_passwords)) {
                    if(!_passwords[0].isEmpty()) {
                        currentPasswordMatch(_passwords[0], _passwords[1]);
                    }
                }
            }
        });
    }

    private boolean validPasswords(String[] passwords) {
        boolean hasDigitTrue = false;
        if(passwords[1].equals("")) {
            mTextInputLayoutNewPassword.setError("Field required!");
            return false;
        }
        /* check if length is between limits */
        if(passwords[1].length() < 8 || passwords[1].length() > 20) {
            mTextInputLayoutNewPassword.setError("Password must be between 8 and 20 characters");
            return false;
        }
        /* it has digits */
        for(char digit : passwords[1].toCharArray()) {
            if(Character.isDigit(digit)) {
                hasDigitTrue = true;
            }
        }
        if(!hasDigitTrue) {
            mTextInputLayoutNewPassword.setError("Password must have digits");
            return false;
        }
        /* it has uppercase/ lowercase letter */
        if(passwords[1].equals(passwords[1].toLowerCase()) || passwords[1].equals(passwords[1].toUpperCase())) {
            mTextInputLayoutNewPassword.setError("Password needs lowercase and uppercase letters");
            return false;
        }
        mTextInputLayoutNewPassword.setErrorEnabled(false);
        if(!passwords[2].equals(passwords[1])) {
            mTextInputLayoutNewPasswordConfirm.setError("Passwords do not match!");
            return false;
        }
        mTextInputLayoutNewPasswordConfirm.setErrorEnabled(false);
        return true;
    }

    private void currentPasswordMatch(String password, final String newPassword) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("TRY", currentUser.getEmail() + password);
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            mTextInputLayoutCurrentPassword.setErrorEnabled(false);
                            saveNewPasswordInDatabase(newPassword);
                        }
                        else {
                            mTextInputLayoutCurrentPassword.setError("Current password not matching!");
                        }
                    }
                });
    }

    private void saveNewPasswordInDatabase(String newPassword) {
        FirebaseUser _user = FirebaseAuth.getInstance().getCurrentUser();

        _user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    logOut();
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
        getActivity().setResult(PASSWORD_CHANGED);
        getActivity().finish();
    }


    @Override
    public void onClick(View v) {
        int _selectedImageView = v.getId();
        Log.d("Selected", _selectedImageView + "");
        switch (_selectedImageView) {
            case R.id.frag_CPassword_IV_ShowCurrentPassword:
                final ImageView _imageViewShowCurrentPassword = mInflater.findViewById(R.id.frag_CPassword_IV_ShowCurrentPassword);
                mShowCurrentPasswordTrue = showHidePassword(mCurrentPasswordEditText, mShowCurrentPasswordTrue, _imageViewShowCurrentPassword);
                break;
            case R.id.frag_CPassword_IV_ShowNewPassword:
                final ImageView _imageViewShowNewPassword = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPassword);
                mShowNewPasswordTrue = showHidePassword(mNewPasswordEditText, mShowNewPasswordTrue, _imageViewShowNewPassword);
                break;
            case R.id.frag_CPassword_IV_ShowNewPasswordConfirm:
                final ImageView _imageViewShowNewPasswordConfirm = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPassword);
                mShowNewPasswordConfirmTrue = showHidePassword(mNewPasswordConfirmEditText, mShowNewPasswordConfirmTrue, _imageViewShowNewPasswordConfirm);
                break;
        }
    }


    private boolean showHidePassword(EditText passwordEditText, boolean mShowPasswordTrue, ImageView showPasswordImageView) {
        if(mShowPasswordTrue) {
            passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordEditText.setSelection(passwordEditText.getText().length());
            showPasswordImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_24px));
            return false;
        }
        else {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordEditText.setSelection(passwordEditText.getText().length());
            showPasswordImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_off_24px));
            return true;
        }
    }
}
