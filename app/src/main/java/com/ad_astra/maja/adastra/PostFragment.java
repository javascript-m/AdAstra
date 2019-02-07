package com.ad_astra.maja.adastra;


import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */

public class PostFragment extends Fragment {

    String ID;
    String text;
    String url;
    int votes = 0;
    boolean isPost;

    ImageView img;
    TextView author;
    TextView content;

    FirebaseFirestore db;
    HomeScreen homeScreen;

    public PostFragment() {
        // Required empty public constructor
    }

    public static PostFragment newInstance(String id, String txt, int v, boolean isPost, String achUrl) {
        Bundle bundle = new Bundle();
        bundle.putString("ID", id);
        bundle.putString("text", txt);
        bundle.putInt("votes", v);
        bundle.putBoolean("isPost", isPost);
        bundle.putString("imgUrl", achUrl);

        PostFragment fragment = new PostFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    private void readBundle(Bundle bundle) {
        if (bundle != null) {
            ID = bundle.getString("ID");
            text = bundle.getString("text");
            votes = bundle.getInt("votes");
            isPost = bundle.getBoolean("isPost");
            url = bundle.getString("imgUrl");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View postFragment = inflater.inflate(R.layout.fragment_post, container, false);

        img = (ImageButton) postFragment.findViewById(R.id.PF_userImg);
        author = (TextView) postFragment.findViewById(R.id.PF_username);
        content = (TextView) postFragment.findViewById(R.id.PF_content);
        db = FirebaseFirestore.getInstance();
        homeScreen = new HomeScreen();

        readBundle(getArguments());

        if (!isPost) {
            // It is an achievement
            if (!url.isEmpty())
                homeScreen.urlImgToHolder(img, url, getResources());
            else {
                String hName = ID.substring(0, 1);
                TextDrawable textDrawable = new TextDrawable(Objects.requireNonNull(getContext()));
                textDrawable.addCustomStyle(hName);
                LayerDrawable layerList = circleBtn(textDrawable, true);

                //TODO: Add border to drawable
                img.setBackground(layerList);
            }

            author.setText(ID);
            content.setText(text);
            return postFragment;
        }


        if (ID != null && !ID.isEmpty()) {
            db.collection("users").document(ID).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                String displayName = task.getResult().get("username").toString();
                                String imgUrl = task.getResult().get("imgUrl").toString();

                                if (!imgUrl.isEmpty())
                                    homeScreen.urlImgToHolder(img, imgUrl, getResources());

                                author.setText(displayName);
                                content.setText(text);
                            }
                        }
                    });
        }
        // Inflate the layout for this fragment
        return postFragment;
    }

    public LayerDrawable circleBtn(Drawable drawMe, boolean isText) {
        LayerDrawable layerList = (LayerDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.habit_btn, null);
        Objects.requireNonNull(layerList).mutate();
        if (isText) layerList.setLayerGravity(0, Gravity.CENTER);
        layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
        layerList.setDrawable(0, drawMe);
        layerList.invalidateSelf();
        return layerList;
    }
}
