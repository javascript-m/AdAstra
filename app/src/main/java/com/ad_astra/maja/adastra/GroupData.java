package com.ad_astra.maja.adastra;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class GroupData extends AppCompatActivity {

    final private String TAG = "GROUP DATA";

    EditText write;
    ImageButton submit;
    String gID;

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    FirebaseAuth mAuth;
    FirebaseUser user;
    String userID;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_data);

        final String groupID = getIntent().getStringExtra(EXTRA_MESSAGE);
        gID = groupID;

        write = (EditText) findViewById(R.id.GD_write);
        submit = (ImageButton) findViewById(R.id.GD_submit);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        userID = user.getUid();
        db = FirebaseFirestore.getInstance();

        loadOldPosts();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = write.getText().toString().trim();
                if (!text.isEmpty()) {
                    fragmentManager = getSupportFragmentManager();
                    fragmentTransaction = fragmentManager.beginTransaction();

                    PostFragment postFragment = PostFragment.newInstance(userID, text, 0);

                    fragmentTransaction.add(R.id.GD_holder, postFragment);
                    fragmentTransaction.commit();
                    write.setText("");

                    //Add to database
                    Map<String, Object> setData = new HashMap<>();
                    setData.put("ID", userID);
                    setData.put("text", text);
                    setData.put("votes", 0);

                    DocumentReference dRef = db.collection("groups").document(groupID).collection("posts").document();
                    try {
                        dRef.set(setData);
                    } catch (Exception e) {
                        Log.d(TAG, "Something went wrong");
                    }
                }
            }
        });
    }

    private void loadOldPosts() {
        fragmentManager = getSupportFragmentManager();
        db.collection("groups").document(gID).collection("posts").get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            fragmentTransaction = fragmentManager.beginTransaction();

                            HashMap data = (HashMap) document.getData();

                            String usID = (String) data.get("ID").toString();
                            String content = (String) data.get("text").toString();
                            int votes = (int) Integer.parseInt(data.get("votes").toString());

                            PostFragment postFragment = PostFragment.newInstance(usID, content, votes);
                            fragmentTransaction.add(R.id.GD_holder, postFragment);
                            fragmentTransaction.commit();
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
    }
}
