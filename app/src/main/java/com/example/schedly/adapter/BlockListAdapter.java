package com.example.schedly.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockListAdapter extends RecyclerView.Adapter<BlockListAdapter.BlockListViewHolder> {
    private ArrayList<String> mDataSet;
    private Context mContext;
    private String mUserID;

    public BlockListAdapter(Context context, ArrayList<String> dataSet, String userID) {
        mContext = context;
        mDataSet = dataSet;
        mUserID = userID;
    }


    @NonNull
    @Override
    public BlockListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RelativeLayout _viewGroup = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_block_list_element, parent, false);

        BlockListViewHolder _vh = new BlockListViewHolder(_viewGroup, parent);
        return _vh;
    }

    @Override
    public void onBindViewHolder(@NonNull BlockListViewHolder holder, int position) {
        String _phoneNumber = mDataSet.get(position);
        holder.updatePNumber(_phoneNumber, position);
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public class BlockListViewHolder extends RecyclerView.ViewHolder {

        private TextView mPNumberTV, mUnblockTV;
        private int mPosition;

        public BlockListViewHolder(@NonNull final View itemView, @NonNull final ViewGroup parent) {
            super(itemView);

            mPNumberTV = itemView.findViewById(R.id.frag_BList_TV_PNumber);
            mUnblockTV = itemView.findViewById(R.id.frag_BList_TV_Unblock);

            mUnblockTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog();
                }
            });
        }

        private void showDialog() {
            new AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.frag_BlockList_DialogTitle))
                    .setMessage(mContext.getString(R.string.frag_BlockList_DialogMessage))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String _phoneNumber = mPNumberTV.getText().toString();

                            Map<String, Object> _deleteNumber = new HashMap<>();
                            _deleteNumber.put(_phoneNumber, FieldValue.delete());
                            FirebaseFirestore.getInstance().collection("blockLists")
                                    .document(mUserID)
                                    .set(_deleteNumber, SetOptions.merge())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mDataSet.remove(mPosition);
                                            BlockListAdapter.this.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("ErrBlockList", e.toString());
                                        }
                                    });
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        }


        public void updatePNumber(String phoneNumber, int position) {
            mPNumberTV.setText(phoneNumber);
            mPosition = position;
        }
    }
}
