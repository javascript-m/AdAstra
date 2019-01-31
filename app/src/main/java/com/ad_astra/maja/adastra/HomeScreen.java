package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//TODO: CITAJ ISPOD
/**
 * 0) Sign up link ne radi
 * 0.5) One funkcije onCreateView pokretat samo kad se radi home screen?
 * 1) my profile progress bar prati onaj progress bar sa lvl-ima
 * 2) debuggirat sve od dodavanja navika nadalje -> mozda cak sve prebacit ONLINE!!
 * 3) dizajn hrpetine stvari
 * 4) u stats da se vidi za svaku naviku pojedinačno i malo bolje opis grafa
 * 5) achievements -> to bi trebalo bit dosta jednostavno a fora izgleda
 * 6) smanjivat velicine slika da sve bude brze
 */


public class HomeScreen extends AppCompatActivity {

    private ViewPager viewPager;
    private FragmentCollectionAdapter adapter;

    //TODO: MAKNUT SVE NEPOTREBNE TOASTO-ve

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        Toolbar toolbar = findViewById(R.id.HS_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Set up viewPager -> FragmentCollectionAdapter.java
        viewPager = findViewById(R.id.HS_pager);
        adapter = new FragmentCollectionAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(2);
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.HS_myProfileBtn:
                startActivity(new Intent(HomeScreen.this, MyProfile.class));
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
                startActivity(new Intent(HomeScreen.this, MainActivity.class));
                break;
        }
        return true;
    }

    public void urlImgToHolder(final View holder, String url, final Resources res) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(url);

        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        RoundedBitmapDrawable icon = RoundedBitmapDrawableFactory.create(res, bmp);
                        icon.setCornerRadius(Math.max(bmp.getWidth(), bmp.getHeight()) / 2.0f);
                        holder.setBackground(icon);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("POST FRAGMENT", "Error loading habit icon.");
            }
        });
    }
}
