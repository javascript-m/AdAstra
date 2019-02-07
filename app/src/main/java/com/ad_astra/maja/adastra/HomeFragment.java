package com.ad_astra.maja.adastra;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class HomeFragment extends Fragment {

    private final String TAG = "HOME FRAGMENT";

    View homeFragment;

    long dateInt;
    int curDayDone = 0;

    // HabitInfo objects for ALL habits
    Map <String, Object> habit_list = new HashMap<>();

    FirebaseAuth mAuth;
    FirebaseUser fbUser;
    User user;
    String userID;
    FirebaseFirestore db;
    FirebaseStorage storage;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeFragment = inflater.inflate(R.layout.fragment_home, container, false);

        //Initialize to today
        dateInt = AddHabit.getMidnight(0);

        //User stuff
        mAuth = FirebaseAuth.getInstance();
        fbUser = mAuth.getCurrentUser();
        userID = fbUser.getUid();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ImageView profilePic = homeFragment.findViewById(R.id.HF_profilePic);
        HomeScreen homeScreen = new HomeScreen();
        homeScreen.urlImgToHolder(profilePic, fbUser.getPhotoUrl().toString(), getResources());

        initializeFragment();

        Button addHabitBtn = homeFragment.findViewById(R.id.HF_add);
        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user.habitList.size() >= 6) {
                    Toast.makeText(getContext(), "Maximum number of habits reached", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(getContext(), AddHabit.class);
                String message = "add habit";
                intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
            }
        });

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

                                //TODO: TESTIRAJ OVAJ DIO am dap
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

        //Select the last one (today)
        dayManager.getTabAt(6).select();
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

            final TextView txtView = (TextView) progressHolder.getChildAt(2*hNum);
            txtView.setVisibility(View.VISIBLE);
            txtView.setTag(hName + "txt");

            habitHolder.getChildAt(2*hNum+1).setVisibility(View.VISIBLE);
            progressHolder.getChildAt(2*hNum+1).setVisibility(View.VISIBLE);

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
                            arrangeValues(btnView, txtView, "notDefined");
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
                arrangeValues(btnView, txtView, "notDefined");
            }

            // Tick and edit info options
            btnView.setOnClickListener(habitClick);
            btnView.setOnLongClickListener(habitLongClick);
            hNum++;
        }

        createDayChooser();
    }

    private void updateHomeScreen() {

        //How many tasks are done today
        curDayDone = 0;

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

                                    if (state.equals("true")) curDayDone++;

                                    try {
                                        arrangeValues(homeFragment.findViewWithTag(hName), (TextView) homeFragment.findViewWithTag(hName+"txt"), state);
                                    } catch (Exception e) {
                                        Log.d(TAG, e.toString());
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
            HabitInfo habitInfo = (HabitInfo) habit_list.get(v.getTag().toString());

            LayerDrawable layerList = (LayerDrawable) v.getBackground();
            int alpha = layerList.findDrawableByLayerId(R.id.habitButtonDone).getAlpha();

            String state;

            if (alpha == 0) {
                state = "true";
                user.exp += 20;
                habitInfo.done += 1;
                curDayDone += 1;
            }
            else {
                user.exp -= 20;
                habitInfo.done -= 1;
                curDayDone -= 1;
                state = "false";
            }

            arrangeValues(v, (TextView) homeFragment.findViewWithTag(v.getTag()+"txt"), state);

            Map <Object, String> setData = new HashMap<>();
            setData.put(v.getTag().toString(), state);
            db.collection("users").document(userID).collection("events").document(Long.toString(dateInt))
                    .set(setData, SetOptions.merge());

            db.collection("users").document(userID).collection("habits").document(v.getTag().toString())
                    .set(habitInfo, SetOptions.merge());
        }
    };

    private View.OnLongClickListener habitLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Intent intent = new Intent(getContext(), AddHabit.class);
            String message = v.getTag().toString();
            intent.putExtra(EXTRA_MESSAGE, message);
            startActivity(intent);
            return false;
        }
    };

    //Updates button style, upper text value and circular progress bar
    public void arrangeValues(final View btn, TextView txt, String state) {
        HabitInfo  habitInfo = (HabitInfo) habit_list.getOrDefault(btn.getTag().toString(), null);
        if (habitInfo != null) {
            //Add upper text
            String upperText = Integer.toString(habitInfo.done) + "/" + Integer.toString(HabitPlan.plan[habitInfo.goal-1][habitInfo.week]);
            txt.setText(upperText);

            //Update progress bar
            ProgressBar progressBar = (ProgressBar) homeFragment.findViewById(R.id.HF_circBar);
            TextView progressText = (TextView) homeFragment.findViewById(R.id.HF_progressTxt);
            if (user.habitList.size() != 0) {
                int value = curDayDone*100 / user.habitList.size();
                if (value > 90) value = 100;
                if (value < 15) value = 0;
                progressBar.setProgress(value);
                progressText.setText(value + "%");
            }
        }

        // Event exp
        Map <String, Object> setExp = new HashMap<>();
        setExp.put("EXP", curDayDone * 20);
        db.collection("users").document(userID).collection("events").document(Long.toString(dateInt))
                .set(setExp, SetOptions.merge());

        // User progress values
        if (user.exp >= user.lvl * 50) {
            user.exp -= user.lvl * 50;
            user.lvl++;
        }
        else if (user.exp < 0) {
            user.lvl--;
            user.exp = user.lvl * 50 + user.exp;
        }
        String lvl = "Lvl. #" + Integer.toString(user.lvl);
        String exp = Integer.toString(user.exp) + "/" + Integer.toString(user.lvl * 50);

        TextView lvlTxt = (TextView) homeFragment.findViewById(R.id.HF_lvl);
        TextView expTxt = (TextView) homeFragment.findViewById(R.id.HF_exp);
        lvlTxt.setText(lvl);
        expTxt.setText(exp);

        Map <String, Object> setData = new HashMap<>();
        setData.put("lvl", user.lvl);
        setData.put("exp", user.exp);
        db.collection("users").document(userID).set(setData, SetOptions.merge());

        // state already familiar
        if (!state.equals("notDefined")) {
            LayerDrawable layerList = (LayerDrawable) btn.getBackground();
            if (state.equals("true")) {
                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(255);
            } else {
                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
            }
            layerList.invalidateSelf();
            btn.setBackground(layerList);
            return;
        }

        final String hName = btn.getTag().toString();
        db.collection("users").document(userID).collection("events").document(Long.toString(dateInt)).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            LayerDrawable layerList;
                            try {
                                layerList = (LayerDrawable) btn.getBackground();
                            } catch (Exception e) {
                                layerList = (LayerDrawable) getResources().getDrawable(R.drawable.habit_btn, null);
                            }

                            String state = task.getResult().getString(hName);

                            if (state != null && state.equals("true")) {
                                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(255);
                            } else {
                                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
                            }
                            layerList.invalidateSelf();
                            btn.setBackground(layerList);
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
