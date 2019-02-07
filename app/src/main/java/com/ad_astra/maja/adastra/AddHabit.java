package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/* ChooseHabit actitvity sa intentom ispred
* */

public class AddHabit extends AppCompatActivity {

    private final String TAG = "ADD HABIT";
    private static final int ADD_IMAGE = 125;

    ImageView hImg;
    EditText hName, hDesc, hTrigger, hReplacement;
    SeekBar hGoal;

    String name, desc, trigger, replacement;
    int goal;
    String activityMode;

    User user;
    String userID;
    HabitInfo EhabitInfo;

    Context context;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;
    Uri uriDownload;
    Uri uriHabitIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        context = (Context) getApplicationContext();

        hImg = (ImageView)findViewById(R.id.AAH_img);
        hName = (EditText)findViewById(R.id.AAH_name);
        hGoal = (SeekBar)findViewById(R.id.AAH_goal);
        hDesc = (EditText)findViewById(R.id.AAH_desc);
        hTrigger = (EditText)findViewById(R.id.AAH_trigger);
        hReplacement = (EditText)findViewById(R.id.AAH_rep);

        // Get message if turned to Edit Habit Info mode
        activityMode = getIntent().getStringExtra(EXTRA_MESSAGE);

        TextView dHabit = (TextView)findViewById(R.id.AAH_delete);
        if (!activityMode.equals("add habit"))
            dHabit.setVisibility(View.VISIBLE);

        setRealtimeUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.AAH_submit:
                if (validateInputs()) {
                    if (activityMode.equals("add habit")) {
                        submitAddChanges();
                        finish();
                        startActivity(new Intent(AddHabit.this, HomeScreen.class));
                    } else {
                        submitEditChanges();
                        finish();
                        startActivity(new Intent(AddHabit.this, HomeScreen.class));
                    }
                } else {
                    return;
                }
                break;
            case R.id.AAH_img:
                chooseImage();
                break;
            case R.id.AAH_delete:
                deleteHabit(activityMode);
                break;
        }
    }

    public void setRealtimeUpdates() {
        db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            user = snapshot.toObject(User.class);
                            if (user != null) {
                                if (!activityMode.equals("add habit")) {
                                    hName.setEnabled(false);
                                    loadHabitData();
                                }
                            }
                        }
                    }
                });
    }

    //Intent that opens image chooser
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select habit icon"), ADD_IMAGE);
    }
    //Adding habit icon to storage
    private void uploadImgToFirebaseStorage() {
        if (uriHabitIcon != null) {
            final StorageReference refPic = storageReference.child(userID+"/"+System.currentTimeMillis()+".jpg");
            UploadTask uploadTask = refPic.putFile(uriHabitIcon);

            Task<Uri> URLtask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return refPic.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        uriDownload = task.getResult();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriHabitIcon = data.getData();
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uriHabitIcon);
                hImg.setImageBitmap(bmp);
                uploadImgToFirebaseStorage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long getMidnight(int day) {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        date.add(Calendar.DATE, day);
        return date.getTime().getTime()/1000;
    }

    private boolean validateInputs() {
        name = hName.getText().toString().trim().toLowerCase();
        desc = hDesc.getText().toString().trim();
        trigger = hTrigger.getText().toString().trim();
        replacement = hReplacement.getText().toString().trim();
        goal = hGoal.getProgress();

        if (name.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (desc.isEmpty()) {
            hDesc.setError("Description is required.");
            hDesc.requestFocus();
            return false;
        }
        if (trigger.isEmpty()) {
            hTrigger.setError("Trigger is required.");
            hTrigger.requestFocus();
            return false;
        }
        if (activityMode.equals("add habit") && user.habitList.contains(name)) {
            hName.setError("This habit already exists");
            hName.requestFocus();
            return false;
        }
        if (!activityMode.equals("add habit") && uriDownload == null) {
            uriDownload = Uri.parse(EhabitInfo.imgUriS);
        }
        return true;
    }

    //Create habitInfo file and add habit name to userInfo file
    private void submitAddChanges() {
        try {
            String downloadUrl = "";
            try {
                downloadUrl = uriDownload.toString();
            } catch (Exception e) {
                Log.d(TAG, "No image");
            }

            final HabitInfo habitInfo = new HabitInfo(context, name, desc, goal, trigger, replacement, getMidnight(0), downloadUrl);
            db.collection("users").document(userID).collection("habits").document(name).set(habitInfo)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, e.toString());
                        }
                    });
            user.habitList.add(name);
            db.collection("users").document(userID).set(user)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, e.toString());
                        }
                    });
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
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

    //EDIT MODE FUNCTIONS
    private void loadHabitData() {
        db.collection("users").document(userID).collection("habits").document(activityMode)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        EhabitInfo = documentSnapshot.toObject(HabitInfo.class);
                        hName.setText(EhabitInfo.name.toUpperCase());
                        hGoal.setProgress(EhabitInfo.goal);
                        hDesc.setText(EhabitInfo.desc);
                        hTrigger.setText(EhabitInfo.trigger);
                        hReplacement.setText(EhabitInfo.replacement);

                        if (!EhabitInfo.imgUriS.isEmpty() && !EhabitInfo.imgUriS.equals("textImage")) {
                            StorageReference httpsReference = storage.getReferenceFromUrl(EhabitInfo.imgUriS);
                            final long ONE_MEGABYTE = 1024 * 1024;
                            httpsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    Drawable habitIcon = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                                    LayerDrawable layerList = circleBtn(habitIcon, false);
                                    hImg.setBackground(layerList);

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Log.d(TAG, "Error loading habit icon.");
                                }
                            });
                        } else {
                            TextDrawable textDrawable = new TextDrawable(AddHabit.this);
                            textDrawable.addCustomStyle(EhabitInfo.name);
                            LayerDrawable layerList = circleBtn(textDrawable, true);
                            hImg.setBackground(layerList);
                        }
                    }
                });
    }

    private void submitEditChanges() {
        try {
            final HabitInfo habitInfo = new HabitInfo(context, name, desc, goal, trigger, replacement, EhabitInfo.startDay, uriDownload.toString());
            db.collection("users").document(userID).collection("habits").document(name).set(habitInfo);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

    }

    private void deleteHabit(String hName) {
        db.collection("users").document(userID).collection("habits").document(hName).delete();

        user.habitList.remove(hName);
        db.collection("users").document(userID).set(user);

        //TODO: Delete all events in the last week that contain habitName
    }
}
