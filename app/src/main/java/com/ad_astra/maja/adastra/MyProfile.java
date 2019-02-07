package com.ad_astra.maja.adastra;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MyProfile extends AppCompatActivity {

    private static final int CHOOSE_IMAGE = 127;
    private final String TAG = "MY PROFILE";

    ImageView profilePic;
    TextView usnameTxt, lvl, pDays, exp;
    ProgressBar lvlBar;
    //TextView emailVer;

    Uri uriProfilePic;
    Uri downloadUrl;
    UploadTask uploadTask;

    User user;
    String userID;
    FirebaseAuth mAuth;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseFirestore db;
    HomeScreen homeScreen;

    //For achievements
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        Toolbar toolbar = findViewById(R.id.MP_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();
        homeScreen = new HomeScreen();

        profilePic = (ImageView)findViewById(R.id.MP_profilePic);
        usnameTxt = (TextView)findViewById(R.id.MP_username);
        lvl = (TextView) findViewById(R.id.MP_lvl);
        pDays = (TextView) findViewById(R.id.MP_pDays);
        exp = (TextView) findViewById(R.id.MP_exp);
        lvlBar = (ProgressBar) findViewById(R.id.MP_lvlBar);
        //emailVer = (TextView)findViewById(R.id.MP_emailVerified);

        loadUserInformation();
        loadAchievements();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(MyProfile.this, MainActivity.class));
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.MP_profilePic:
                chooseImage();
                break;
            case R.id.MP_back:
                finish();
                startActivity(new Intent(MyProfile.this, HomeScreen.class));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuLogout:
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(MyProfile.this, MainActivity.class));
                break;
        }
        return true;
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select profile image"), CHOOSE_IMAGE);
    }

    private void uploadImgToFirebaseStorage() {
        if (uriProfilePic != null) {
            final StorageReference refProfilePic = storageReference.child(userID+"/profile_pic.jpg");
            uploadTask = refProfilePic.putFile(uriProfilePic);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        Toast.makeText(MyProfile.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return refProfilePic.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        downloadUrl = task.getResult();
                        submitUserInfo();
                    } else {
                        Log.d(TAG, "Error getting download url");
                    }
                }
            });
        }
    }

    private void submitUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();

        if(user != null && downloadUrl != null) {
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUrl)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Profile updated");
                        loadUserInformation();
                    } else {
                        Log.d(TAG, "Update failed");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MyProfile.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            Map<String, Object> setData = new HashMap<>();
            setData.put("imgUrl", downloadUrl.toString());
            db.collection("users").document(userID).set(setData, SetOptions.merge())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // TODO: Stavit progress bar da korisnici pricekaju upload slike (inace rijesit probleme sa učivatavnjem)
                            homeScreen.urlImgToHolder(profilePic, downloadUrl.toString(), getResources());
                            Toast.makeText(MyProfile.this, "USPJEH", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadUserInformation() {
        final FirebaseUser fbUser = mAuth.getCurrentUser();

        if (fbUser != null) {
            if (fbUser.getPhotoUrl() != null) {
                homeScreen.urlImgToHolder(profilePic, fbUser.getPhotoUrl().toString(), getResources());
            }

            if (fbUser.getDisplayName() != null) {
                usnameTxt.setText(fbUser.getDisplayName());
            }
        }

        db.collection("users").document(userID).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            user = task.getResult().toObject(User.class);

                            if (user != null) {
                                String lvlText = "#" + Integer.toString(user.lvl);
                                String expText = Integer.toString(user.exp) + "/" + Integer.toString(user.lvl * 50);
                                lvl.setText(lvlText);
                                exp.setText(expText);
                                pDays.setText(Integer.toString(user.pDays));
                                lvlBar.setProgress(user.exp * 100 / (user.lvl*50));
                            }
                        }
                    }
                });
    }

    private void loadAchievements() {
        fragmentManager = getSupportFragmentManager();

        db.collection("users").document(userID).collection("achievements").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                fragmentTransaction = fragmentManager.beginTransaction();

                                HashMap data = (HashMap) document.getData();

                                String title = (String) data.get("title").toString();
                                String desc = (String) data.get("description").toString();
                                String imgUrl = (String) data.get("url").toString();

                                //PostFragment is used to display Achievements (They have the same GUI)
                                PostFragment achFragment = PostFragment.newInstance(title, desc, 0, false, imgUrl);
                                fragmentTransaction.add(R.id.MP_holder, achFragment);
                                fragmentTransaction.commit();
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriProfilePic = data.getData();
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uriProfilePic);
                Drawable profileDrawable = new BitmapDrawable(getResources(), bmp);
                uploadImgToFirebaseStorage();
            } catch (Exception e) {
                Toast.makeText(homeScreen, e.toString(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}
