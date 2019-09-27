package com.example.schedly.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.adapter.BlockListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hbb20.CountryCodePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockListFragment extends Fragment {

    private FragmentActivity mActivity;
    private View mInflater;
    private RecyclerView mRecyclerView;
    private BlockListAdapter mAdapter;
    private String mUserID;
    private ArrayList<String> mDataSet = new ArrayList<>();
    private int mCounter;
    private CountryCodePicker mCCP;
    private EditText mEditTextCarrierNumber;
    private boolean mValidNumber;

    public BlockListFragment(String userID) {
        mUserID = userID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_block_list, container, false);
        return mInflater;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle("Block List");
    }

    @Override
    public void onStart() {
        super.onStart();
        getBlockedNumbers();
    }

    private void getBlockedNumbers() {
        mCounter = 0;
        mDataSet.clear();

        FirebaseFirestore.getInstance().collection("blockLists")
                .document(mUserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult() != null && task.getResult().exists()) {
                            Map<String, Object> _PNumbers = task.getResult().getData();
                            for(Map.Entry<String, Object> _PNumber : _PNumbers.entrySet()) {
                                mDataSet.add(mCounter++, _PNumber.getKey());
                                Log.d("Logged", _PNumber.getKey());
                            }
                        }
                        Log.d("Logged", "Err");
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        setRecyclerView();
        Button _buttonAdd = mInflater.findViewById(R.id.frag_BList_BUT_Add);
        _buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumberToBlockList();
            }
        });
        TextView _addTV = mInflater.findViewById(R.id.frag_BList_TV_Add);
        _addTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumberToBlockList();
            }
        });
    }

    private void addNumberToBlockList() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mInflater.getContext());
        LayoutInflater inflater = mActivity.getLayoutInflater();
        final View _dialogLayout = inflater.inflate(R.layout.fragment_block_number_dialog, null);
        builder.setView(_dialogLayout);
        builder.setTitle(R.string.frag_BlockList_DialogBlock_Title);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(mValidNumber) {
                    saveBlockedPhoneNumber(mCCP.getFullNumberWithPlus());
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.dialog_phone_number_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog _dialog = builder.create();
        _dialog.show();
        setElements(_dialogLayout);
    }

    private void saveBlockedPhoneNumber(final String fullNumberWithPlus) {
        Map<String, Object> _blockNumber = new HashMap<>();
        _blockNumber.put(fullNumberWithPlus, true);
        FirebaseFirestore.getInstance().collection("blockLists")
                .document(mUserID)
                .set(_blockNumber, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mDataSet.add(mCounter++, fullNumberWithPlus);
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void setRecyclerView() {
        /* RecyclerView */
        mRecyclerView = mInflater.findViewById(R.id.frag_BList_RV);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mInflater.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new BlockListAdapter(mInflater.getContext(), mDataSet, mUserID);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setElements(View dialogLayout) {
        mCCP = dialogLayout.findViewById(R.id.dialog_settings_BL_CPNumber_cpp);
        mEditTextCarrierNumber = dialogLayout.findViewById(R.id.dialog_settings_BL_CPNumber_ET_carrierNumber);
        mCCP.registerCarrierNumberEditText(mEditTextCarrierNumber);

        mCCP.setPhoneNumberValidityChangeListener(new CountryCodePicker.PhoneNumberValidityChangeListener() {
            @Override
            public void onValidityChanged(boolean isValidNumber) {
                mValidNumber = isValidNumber;
            }
        });
    }
}
