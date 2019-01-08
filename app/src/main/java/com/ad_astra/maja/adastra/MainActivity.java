package com.ad_astra.maja.adastra;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/*url slika kako bi se prikazivale u krugovima
* Na papiru odvojenom je mapa kako treba izgledat baza
* */

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(MainActivity.this, HomeScreen.class));
        }
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.MA_SUbtn:
                startActivity(new Intent(MainActivity.this, SignUp.class));
                break;
            case R.id.MA_LIbtn:
                startActivity(new Intent(MainActivity.this, LogIn.class));
                break;
        }
    }
}
