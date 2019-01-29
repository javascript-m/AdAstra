package com.ad_astra.maja.adastra;


import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupButton extends Fragment {

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser user;
    String userID;

    private String name, admin, url, ID;
    private boolean join;

    private TextView nameText;
    private TextView adminText;
    private ImageView img;
    private Button joinBtn;

    public static GroupButton newInstance(String sName, String sAdmin, String sUrl, String gID, boolean bJoin) {
        Bundle bundle = new Bundle();
        bundle.putString("name", sName);
        bundle.putString("admin", sAdmin);
        bundle.putString("url", sUrl);
        bundle.putString("ID", gID);
        bundle.putBoolean("join", bJoin);

        GroupButton fragment = new GroupButton();
        fragment.setArguments(bundle);

        return fragment;
    }

    private void readBundle(Bundle bundle) {
        if (bundle != null) {
            name = bundle.getString("name");
            admin = bundle.getString("admin");
            url = bundle.getString("url");
            ID = bundle.getString("ID");
            join = bundle.getBoolean("join");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_button, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        userID = user.getUid();

        nameText = (TextView) view.findViewById(R.id.FGB_title);
        adminText = (TextView) view.findViewById(R.id.FGB_desc);
        img = (ImageView) view.findViewById(R.id.FGB_img);
        joinBtn = (Button) view.findViewById(R.id.FGB_join);

        readBundle(getArguments());

        if (name != null && !name.isEmpty())
            nameText.setText(name.toUpperCase());

        if (admin != null && !admin.isEmpty())
            adminText.setText("Created by " + admin);

        if (url != null && !url.isEmpty()) {
            //ADD IMAGE TO IMAGEVIEW
        }

        if (join) {
            joinBtn.setVisibility(View.VISIBLE);

            joinBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, Object> setData = new HashMap<>();
                    setData.put("name", user.getDisplayName());
                    db.collection("groups").document(ID).collection("users").document(userID).set(setData)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // TODO instead change button style to joined and a tick
                                joinBtn.setVisibility(View.INVISIBLE);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });
        }

        return view;
    }
}
