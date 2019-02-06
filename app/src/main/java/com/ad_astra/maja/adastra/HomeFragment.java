package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

//TODO: Poboljšat praćenje datuma / dana u tjednu

public class HomeFragment extends Fragment {

    private final String TAG = "HOME FRAGMENT";

    View homeFragment;
    long dateInt;

    // HabitInfo objects for ALL habits
    Map <String, Object> habit_list = new HashMap<>();

    FirebaseAuth mAuth;
    FirebaseUser fbUser;
    User user;
    String userID;
    FirebaseFirestore db;
    FirebaseStorage storage;

    TextView test;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeFragment = inflater.inflate(R.layout.fragment_home, container, false);

        test = homeFragment.findViewById(R.id.HF_todoTitle);

        //Initialize to today
        dateInt = AddHabit.getMidnight(0);

        //User stuff
        mAuth = FirebaseAuth.getInstance();
        fbUser = mAuth.getCurrentUser();
        userID = fbUser.getUid();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeFragment();

        // Inflate the layout for this fragment
        return homeFragment;
    }

    private void initializeFragment() {
        //Track user_info file
        db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot,
                                        @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            user = documentSnapshot.toObject(User.class);
                        } else {
                            Log.d(TAG, "No user data found");
                        }
                    }
                });

        //Get habit data
        db.collection("users").document(userID).collection("habits").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                HabitInfo habitInfo = document.toObject(HabitInfo.class);

                                //TODO: TESTIRAJ OVAJ DIO
                                // Check and update current habit state
                                long weekTime = 604800; // 7 * 24 * 60 * 60

                                if (dateInt - habitInfo.startDay >= weekTime) {
                                    // Moved to next week
                                    if (dateInt - habitInfo.startDay < 2*weekTime) {
                                        habitInfo.week += 1;
                                        habitInfo.startDay += weekTime;
                                        habitInfo.done = 0;
                                    } else { // Skipped a week completely
                                        habitInfo.week -= 1;
                                        habitInfo.startDay = dateInt;
                                        habitInfo.done = 0;
                                    }

                                    if (habitInfo.week >= 10) {
                                        // TODO: FINISHED 10 weeks, izbrisi i dodaj achievement
                                    } else if (habitInfo.week < 0) {
                                        habitInfo.week = 0;
                                    }

                                    db.collection("users").document(userID).collection("habits").document(habitInfo.name)
                                            .set(habitInfo, SetOptions.merge());
                                }
                                habit_list.put(habitInfo.name, habitInfo);
                            }
                            if (user != null)
                                createHomeScreen();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void createDayChooser() {
        final TabLayout dayManager = (TabLayout) homeFragment.findViewById(R.id.HF_week);

        //What day is today (set chooser name)
        int today = getCurrentDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));

        //Set letters of dayManager to match the dates
        int managerPos = 6;
        for (int i=today; i >= 0; i--) {
            Objects.requireNonNull(dayManager.getTabAt(managerPos)).setText(HabitPlan.week_days[i]);
            managerPos--;
        }
        for (int i=6; i > today; i--) {
            Objects.requireNonNull(dayManager.getTabAt(managerPos)).setText(HabitPlan.week_days[i]);
            managerPos--;
        }

        //Select the last one (today)
        dayManager.getTabAt(6).select();

        //Make screen update when tab is selected
        dayManager.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                dateInt = AddHabit.getMidnight(tab.getPosition() - 6);

                Map <String, Object> setData = new HashMap<>();
                setData.put("dateInt", dateInt);
                db.collection("users").document(userID).collection("events").document(Long.toString(dateInt))
                        .set(setData, SetOptions.merge())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful())
                                    updateHomeScreen();
                            }
                        });
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void createHomeScreen() {
        LinearLayout habitHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_habitHolder);
        LinearLayout progressHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_progressHolder);

        //Show buttons with upper text and add background images/text
        int hNum = 0;
        for (final String hName : user.habitList) {

            final ImageButton btnView = (ImageButton) habitHolder.getChildAt(2*hNum);
            btnView.setVisibility(View.VISIBLE);
            btnView.setTag(hName);
            final View txtView = progressHolder.getChildAt(2*hNum);
            txtView.setVisibility(View.VISIBLE);
            txtView.setTag(hName + "txt");

            HabitInfo habitInfo = (HabitInfo) habit_list.getOrDefault(hName, null);

            if (habitInfo != null && habitInfo.imgUriS != null && !habitInfo.imgUriS.isEmpty()) {
                StorageReference httpsReference = storage.getReferenceFromUrl(habitInfo.imgUriS);

                final long ONE_MEGABYTE = 1024 * 1024;
                httpsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Drawable habitIcon = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length));

                        try {
                            LayerDrawable layerList = circleBtn(habitIcon, false);
                            btnView.setBackground(layerList);
                            arrangeBtnStyle(btnView, "notDefined");
                        } catch (Exception e) {
                            Log.d(TAG, "Habit icon display failed.");
                        }
                    }
                });
            } else {
                TextDrawable textDrawable = new TextDrawable(Objects.requireNonNull(getContext()));
                textDrawable.addCustomStyle(hName);
                LayerDrawable layerList = circleBtn(textDrawable, true);
                btnView.setBackground(layerList);
            }

            // Tick and edit info options
            btnView.setOnClickListener(habitClick);
            //btnView.setOnLongClickListener(habitLongClick);
            hNum++;
        }

        // All habit views are created
        if (hNum >= user.habitList.size())
            createDayChooser();
    }

    private void updateHomeScreen() {
        db.collection("users").document(userID).collection("events").document(Long.toString(dateInt)).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            Map<String, Object> data;
                            data = Objects.requireNonNull(task.getResult()).getData();
                            if (data != null) {
                                for (String hName : user.habitList) {
                                    String state = data.getOrDefault(hName, "false").toString();

                                    try {
                                        arrangeBtnStyle(homeFragment.findViewWithTag(hName), state);
                                    } catch (Exception e) {
                                        test.setText("S"+e.toString());
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private View.OnClickListener habitClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LayerDrawable layerList = (LayerDrawable) v.getBackground();
            int alpha = layerList.findDrawableByLayerId(R.id.habitButtonDone).getAlpha();

            String state;

            if (alpha == 0) state = "true";
            else state = "false";
            arrangeBtnStyle(v, state);

            Map <Object, String> setData = new HashMap<>();
            setData.put(v.getTag().toString(), state);

            db.collection("users").document(userID).collection("events").document(Long.toString(dateInt))
                    .set(setData, SetOptions.merge());
        }
    };

    public void arrangeBtnStyle(final View v, String state) {
        if (!state.equals("notDefined")) {
            LayerDrawable layerList = (LayerDrawable) v.getBackground();
            if (state.equals("true")) {
                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(255);
            } else {
                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
            }
            layerList.invalidateSelf();
            v.setBackground(layerList);
            return;
        }

        final String hName = v.getTag().toString();
        db.collection("users").document(userID).collection("events").document(Long.toString(dateInt)).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            LayerDrawable layerList;
                            try {
                                layerList = (LayerDrawable) v.getBackground();
                            } catch (Exception e) {
                                layerList = (LayerDrawable) getResources().getDrawable(R.drawable.habit_btn, null);
                            }

                            String state = task.getResult().get(hName).toString();

                            if (state.equals("true")) {
                                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(255);
                            } else {
                                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
                            }
                            layerList.invalidateSelf();
                            v.setBackground(layerList);
                        }
                    }
                });
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

    private int getCurrentDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            case Calendar.SATURDAY:
                return 5;
            case Calendar.SUNDAY:
                return 6;
        }
        return 0;
    }

}
