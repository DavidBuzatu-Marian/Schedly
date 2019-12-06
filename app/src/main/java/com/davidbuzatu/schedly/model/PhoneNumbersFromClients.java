package com.davidbuzatu.schedly.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class PhoneNumbersFromClients {

    private FirebaseUser firebaseUser;
    private DocumentSnapshot phoneNumbers;
    private static PhoneNumbersFromClients phoneNumbersFromClients;

    private PhoneNumbersFromClients(FirebaseUser user) {
        firebaseUser = user;
    }
    public static PhoneNumbersFromClients getInstance() {
        if(phoneNumbersFromClients == null ) {
            phoneNumbersFromClients = new PhoneNumbersFromClients(FirebaseAuth.getInstance().getCurrentUser());
        }
        return phoneNumbersFromClients;
    }

    public Task<DocumentSnapshot> getPhoneNumbers() {
        return FirebaseFirestore.getInstance().collection("phoneNumbersFromClients").document(firebaseUser.getUid()).get();
    }


    public void increaseNrOfApp(Map<String, Object> infoForThisUser) {
        FirebaseFirestore.getInstance().collection("phoneNumbersFromClients")
                .document(firebaseUser.getUid())
                .set(infoForThisUser, SetOptions.merge());
    }
}
