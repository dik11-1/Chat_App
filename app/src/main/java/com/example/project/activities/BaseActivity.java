package com.example.project.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.project.utilities.Constants;
import com.example.project.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    private static boolean isInitialized = false;
    private static DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isInitialized) {
            PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID));

            ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleListener());
            isInitialized = true;
        }
    }

    public static void clearUserReference() {
        documentReference = null;
        isInitialized = false;
    }

    public static class AppLifecycleListener implements DefaultLifecycleObserver {

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            if (documentReference != null) {
                documentReference.update(Constants.KEY_AVAILABILITY, 1);
            }
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            if (documentReference != null) {
                documentReference.update(Constants.KEY_AVAILABILITY, 0);
            }
        }
    }
}
