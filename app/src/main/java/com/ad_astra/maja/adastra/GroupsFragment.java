package com.ad_astra.maja.adastra;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.ViewGroupUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Objects;
import static com.google.firebase.firestore.FieldPath.documentId;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupsFragment extends Fragment {

    final String TAG = "GROUPS FRAGMENT";

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String userID;
    User user;
    HomeScreen homeScreen;

    TextView title;
    Button addGroup;

    ImageView profilePic;
    TextView lvlTxt, expTxt;

    public GroupsFragment() {
        // Required empty public constructor
    }

    public void appendToList(final GroupInfo groupInfo, int ID, boolean join) {
        try {
            GroupButton gBtn = GroupButton.newInstance(groupInfo.name, groupInfo.admin, groupInfo.imgUrl, groupInfo.groupID, join);

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.add(ID, gBtn);
            fragmentTransaction.commit();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View groupsFragment = inflater.inflate(R.layout.fragment_groups, container, false);

        title = (TextView) groupsFragment.findViewById(R.id.GF_gTitle);
        addGroup = (Button) groupsFragment.findViewById(R.id.GF_addG);
        profilePic = (ImageView) groupsFragment.findViewById(R.id.GF_profilePic);
        lvlTxt = (TextView) groupsFragment.findViewById(R.id.GF_lvl);
        expTxt = (TextView) groupsFragment.findViewById(R.id.GF_exp);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        homeScreen = new HomeScreen();

        setRealtimeUpdates();

        db.collection("users").document(userID).collection("my_groups").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String group_id = document.getId();
                                try {
                                    db.collection("groups").document(group_id).get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    GroupInfo groupInfo = task.getResult().toObject(GroupInfo.class);
                                                    try {
                                                        appendToList(groupInfo, R.id.GF_holder, false);
                                                    } catch (Exception e) {
                                                        Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), e.toString(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                });

        addGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ExploreGroups.class));
            }
        });

        // Inflate the layout for this fragment
        return groupsFragment;
    }

    //Real-time updates listener (checks if database state has changed)
    public void setRealtimeUpdates() {
        final FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser != null) {
            if (fbUser.getPhotoUrl() != null)
                homeScreen.urlImgToHolder(profilePic, fbUser.getPhotoUrl().toString(), getResources());
        }

        db.collection("users").document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            user = task.getResult().toObject(User.class);
                            if (user != null) {
                                String lvl = "Lvl. #" + Integer.toString(user.lvl);
                                String exp = Integer.toString(user.exp) + "/" + Integer.toString(user.lvl * 50);
                                lvlTxt.setText(lvl);
                                expTxt.setText(exp);
                            }
                        }
                    }
                });
    }
}
