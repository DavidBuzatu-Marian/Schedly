package com.davidbuzatu.schedly.fragment;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.activity.SettingsActivity;
import com.davidbuzatu.schedly.model.LogOut;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment implements View.OnClickListener {
    private FragmentActivity mActivity;
    private View mInflater;
    private TextInputLayout mTextInputLayoutCurrentPassword,
            mTextInputLayoutNewPassword,
            mTextInputLayoutNewPasswordConfirm;
    private boolean mShowCurrentPasswordTrue = false,
            mShowNewPasswordTrue = false,
            mShowNewPasswordConfirmTrue = false;
    private EditText mCurrentPasswordEditText,
            mNewPasswordEditText,
            mNewPasswordConfirmEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_change_password, container, false);
        return mInflater;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_password_bar_title));
    }

    @Override
    public void onStart() {
        super.onStart();

        setUpImageViews();
        setUpTILayouts();
        setUpETexts();
        setUpButtonSave();
    }

    private void setUpButtonSave() {
        Button _saveChangesButton = mInflater.findViewById(R.id.frag_CPassword_BUT_saveChanges);
        _saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkValidPasswords();
            }
        });
    }

    private void setUpETexts() {
        mCurrentPasswordEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_CurrentPassword);
        mNewPasswordEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_NewPassword);
        mNewPasswordConfirmEditText = mInflater.findViewById(R.id.frag_CPassword_TIET_NewPasswordConfirm);
    }

    private void setUpTILayouts() {
        mTextInputLayoutCurrentPassword = mInflater.findViewById(R.id.frag_CPassword_TIL_CurrentPassword);
        mTextInputLayoutNewPassword = mInflater.findViewById(R.id.frag_CPassword_TIL_NewPassword);
        mTextInputLayoutNewPasswordConfirm = mInflater.findViewById(R.id.frag_CPassword_TIL_NewPasswordConfirm);
    }

    private void setUpImageViews() {
        ImageView mCurrentPasswordShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowCurrentPassword);
        mCurrentPasswordShowImageView.setOnClickListener(this);
        ImageView mNewPasswordShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPassword);
        mNewPasswordShowImageView.setOnClickListener(this);
        ImageView mNewPasswordConfirmShowImageView = mInflater.findViewById(R.id.frag_CPassword_IV_ShowNewPasswordConfirm);
        mNewPasswordConfirmShowImageView.setOnClickListener(this);
    }

    private void checkValidPasswords() {
        String[] _passwords = new String[3];
        _passwords[0] = mCurrentPasswordEditText.getText().toString();
        _passwords[1] = mNewPasswordEditText.getText().toString();
        _passwords[2] = mNewPasswordConfirmEditText.getText().toString();
        boolean hasDigitTrue = false;
        if (_passwords[0].equals("")) {
            mTextInputLayoutCurrentPassword.setError("Field required!");
            return;
        }
        if (_passwords[1].equals("")) {
            mTextInputLayoutNewPassword.setError("Field required!");
            return;
        }
        if (_passwords[1].length() < 8 || _passwords[1].length() > 20) {
            mTextInputLayoutNewPassword.setError("Password must be between 8 and 20 characters");
            return;
        }
        for (char digit : _passwords[1].toCharArray()) {
            if (Character.isDigit(digit)) {
                hasDigitTrue = true;
            }
        }
        if (!hasDigitTrue) {
            mTextInputLayoutNewPassword.setError("Password must have digits");
            return;
        }
        if (_passwords[1].equals(_passwords[1].toLowerCase()) || _passwords[1].equals(_passwords[1].toUpperCase())) {
            mTextInputLayoutNewPassword.setError("Password needs lowercase and uppercase letters");
            return;
        }
        mTextInputLayoutNewPassword.setErrorEnabled(false);
        if (!_passwords[2].equals(_passwords[1])) {
            mTextInputLayoutNewPasswordConfirm.setError("Passwords do not match!");
            return;
        }
        mTextInputLayoutNewPasswordConfirm.setErrorEnabled(false);
        currentPasswordMatch(_passwords[0], _passwords[1]);
    }

    private void currentPasswordMatch(String password, final String newPassword) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mTextInputLayoutCurrentPassword.setErrorEnabled(false);
                            saveNewPasswordInDatabase(newPassword, currentUser);
                        } else {
                            mTextInputLayoutCurrentPassword.setError("Current password not matching!");
                        }
                    }
                });
    }

    private void saveNewPasswordInDatabase(String newPassword, FirebaseUser currentUser) {
        currentUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    LogOut _logOut = new LogOut(mActivity);
                    _logOut.LogOutFromApp(true);
                } else {
                    Toast.makeText(mActivity,  "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    @Override
    public void onClick(View v) {
        int _selectedImageView = v.getId();
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
        if (mShowPasswordTrue) {
            passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordEditText.setSelection(passwordEditText.getText().length());
            showPasswordImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_24px));
            return false;
        } else {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordEditText.setSelection(passwordEditText.getText().length());
            showPasswordImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_visibility_off_24px));
            return true;
        }
    }
}
