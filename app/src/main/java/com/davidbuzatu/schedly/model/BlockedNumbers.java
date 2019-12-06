package com.davidbuzatu.schedly.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BlockedNumbers {

    private static BlockedNumbers blockedNumbers;
    private DocumentSnapshot blockedPhoneNumbers;
    private String userID = User.getInstance().getUid();

    public static BlockedNumbers getInstance() {
        if(blockedNumbers == null) {
            blockedNumbers = new BlockedNumbers();
        }

        return blockedNumbers;
    }

    public Task<DocumentSnapshot> getBlockedNumbers() {
        return FirebaseFirestore.getInstance()
                .collection("blockLists")
                .document(userID)
                .get();
    }
}
