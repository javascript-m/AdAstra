package com.ad_astra.maja.adastra;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ExploreGroups extends AppCompatActivity {

    final private String TAG = "EXPLORE GROUPS";

    FirebaseAuth mAuth;
    String userID;
    FirebaseFirestore db;

    TextView title;
    final List<String> myGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_groups);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        title = (TextView) findViewById(R.id.EG_title);

        // Get groups user has already joined
        db.collection("users").document(userID).collection("my_groups").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                myGroups.add(document.getId());
                            }
                            findAllGroups();
                        }
                    }
                });
    }

    public void appendToList(final GroupInfo groupInfo, int ID, boolean join) {
        //If user has already joined this group, don't show 'JOIN' option
        if (myGroups.contains(groupInfo.groupID)) join = false;

        //Add group fragment to
        try {
            GroupButton gBtn = GroupButton.newInstance(groupInfo.name, groupInfo.admin, groupInfo.imgUrl, groupInfo.groupID, join);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.add(ID, gBtn);
            fragmentTransaction.commit();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    //Find all not-joined groups in db
    private void findAllGroups() {
        CollectionReference colRef = db.collection("groups");
        colRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        appendToList(document.toObject(GroupInfo.class), R.id.EG_holder, true);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ExploreGroups.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
