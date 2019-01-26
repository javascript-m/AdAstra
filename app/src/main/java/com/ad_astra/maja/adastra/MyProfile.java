package com.ad_astra.maja.adastra;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

/*
* Stavit progress bar da korisnici pricekaju upload slike (inace rijesit probleme sa učivatavnjem)
* */

public class MyProfile extends AppCompatActivity {

    private static final int CHOOSE_IMAGE = 127;
    private final String TAG = "MY PROFILE";

    ImageView profilePic;
    TextView usnameTxt;
    //TextView emailVer;

    Uri uriProfilePic;
    Uri downloadUrl;
    UploadTask uploadTask;

    String userID;
    FirebaseAuth mAuth;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseFirestore db;

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

        profilePic = (ImageView)findViewById(R.id.MP_profilePic);
        usnameTxt = (TextView)findViewById(R.id.MP_username);
        //emailVer = (TextView)findViewById(R.id.MP_emailVerified);

        loadUserInformation();
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
                        Toast.makeText(MyProfile.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MyProfile.this, "UPDATE FAILED", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MyProfile.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    //For glide -> image upload
    RequestOptions glideOptions = new RequestOptions().centerCrop();

    private void loadUserInformation() {
        final FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .apply(glideOptions)
                        .into(profilePic);
            }

            if (user.getDisplayName() != null) {
                usnameTxt.setText(user.getDisplayName());
            }

            //U buducnosti osposobit
            /*if (user.isEmailVerified()) {
                emailVer.setText("");
            } else {
                emailVer.setText(R.string.emailVerification);
                emailVer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(MyProfile.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        FirebaseAuth.getInstance().signOut();
                        finish();
                        startActivity(new Intent(MyProfile.this, MainActivity.class));
                    }
                });
            }*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriProfilePic = data.getData();
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uriProfilePic);
                Drawable profileDrawable = new BitmapDrawable(getResources(), bmp);

                profilePic.setBackground(profileDrawable);
                uploadImgToFirebaseStorage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
