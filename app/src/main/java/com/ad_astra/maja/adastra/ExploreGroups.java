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

public class ExploreGroups extends AppCompatActivity {

    final private String TAG = "EXPLORE GROUPS";

    FirebaseAuth mAuth;
    String userID;
    FirebaseFirestore db;

    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_groups);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        title = (TextView) findViewById(R.id.EG_title);

        findAllGroups();
    }

    private void appendToList(final GroupInfo groupInfo) {
        // TODO smisli način da se ne prikazuju korisnikove grupe
        try {
            GroupButton gBtn = GroupButton.newInstance(groupInfo.name, groupInfo.admin, groupInfo.imgUrl, groupInfo.groupID, true);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.add(R.id.EG_holder, gBtn);
            fragmentTransaction.commit();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    private void findAllGroups() {
        CollectionReference colRef = db.collection("groups");
        colRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        appendToList(document.toObject(GroupInfo.class));
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
