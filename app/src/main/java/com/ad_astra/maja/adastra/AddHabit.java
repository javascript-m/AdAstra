package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/* Unutar validate inputs (update)
GOAL TREBA ISCITAVAT PROGRESS BAR
Mozes obrisat onaj dolje add success listener
treba na UI dodat hReplacement i dodat ChooseHabit actitvity sa intentom ispred
* */

public class AddHabit extends AppCompatActivity {

    private static final int ADD_IMAGE = 125;

    ImageView hImg;
    EditText hName, hDesc, hTrigger, hReplacement;
    SeekBar hGoal;

    String name, desc, trigger, replacement;
    int goal;
    String activityMode;

    User user;
    String userID;

    Context context;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;
    Uri uriDownload;
    Uri uriHabitIcon;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;


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
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        hImg = (ImageView)findViewById(R.id.AAH_img);
        hName = (EditText)findViewById(R.id.AAH_name);
        hGoal = (SeekBar)findViewById(R.id.AAH_goal);
        hDesc = (EditText)findViewById(R.id.AAH_desc);
        hTrigger = (EditText)findViewById(R.id.AAH_trigger);
        //hReplacement = (EditText)findViewById(R.id.AAH_replacement);

        // Get message if turned to Edit Habit Info mode
        activityMode = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (!activityMode.equals("add habit")) {
            hName.setEnabled(false);
            loadHabitData();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.AAH_submit:
                if (!validateInputs()) return;
                if (activityMode.equals("add habit")) {
                    submitAddChanges();
                } else {
                    submitEditChanges();
                }
                finish();
                startActivity(new Intent(AddHabit.this, HomeScreen.class));
                break;
            case R.id.AAH_img:
                chooseImage();
                break;
        }
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

    public long getMidnight(int day) {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        date.add(Calendar.DAY_OF_MONTH, day);

        return date.getTime().getTime()/1000;
    }

    private boolean validateInputs() {
        name = hName.getText().toString().trim();
        desc = hDesc.getText().toString().trim();
        trigger = hTrigger.getText().toString().trim();
        replacement = "replacement";
        goal = 2;
        //Get GOAL and REPLACEMENT VALUE

        //KAD SPREMAS SVE TO LOWERCASE I STIRP(TRIM)

        if (name.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (user.habitList.contains(name)) {
            hName.setError("This habit already exists");
            hName.requestFocus();
            return false;
        }
        if (uriDownload == null) {
            uriDownload = Uri.parse("textImage");
            return false;
        }
        return true;
    }

    //Create habitInfo file and add habit name to userInfo file
    private void submitAddChanges() {
        final HabitInfo habitInfo = new HabitInfo(context, name, desc, goal, trigger, replacement, getMidnight(0), uriDownload.toString());
        db.collection("users").document(userID).collection("habits").document(name).set(habitInfo);

        user.habitList.add(name);
        db.collection("users").document(userID).set(user);
    }


    public LayerDrawable circleBtn(Drawable drawMe, boolean isText) {
        LayerDrawable layerList = (LayerDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.habit_btn, null);
        Objects.requireNonNull(layerList).mutate();
        if (isText) layerList.setLayerGravity(0, Gravity.CENTER);
        layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
        layerList.findDrawableByLayerId(R.id.habitButtonSkipped).setAlpha(0);
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
                        HabitInfo habitInfo = documentSnapshot.toObject(HabitInfo.class);
                        hName.setText(habitInfo.name.toUpperCase());
                        hGoal.setProgress(habitInfo.goal);
                        hDesc.setText(habitInfo.desc);
                        hTrigger.setText(habitInfo.trigger);
                        //hReplacement.setText(habitInfo.replacement);

                        if (!habitInfo.imgUriS.isEmpty() && !habitInfo.imgUriS.equals("textImage")) {
                            StorageReference httpsReference = storage.getReferenceFromUrl(habitInfo.imgUriS);
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
                                    Log.d("ADD HABIT", "Error loading habit icon.");
                                }
                            });
                        } else {
                            TextDrawable textDrawable = new TextDrawable(AddHabit.this);
                            textDrawable.addCustomStyle(habitInfo.name);
                            LayerDrawable layerList = circleBtn(textDrawable, true);
                            hImg.setBackground(layerList);
                        }
                    }
                });
    }

    private void submitEditChanges() {
        //U slucaju dodavanje slike promijeni url
    }
}
