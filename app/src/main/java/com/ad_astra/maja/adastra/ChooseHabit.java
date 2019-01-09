package com.ad_astra.maja.adastra;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;

public class ChooseHabit extends AppCompatActivity {

    Switch switchBtn;
    ScrollView startList, quitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_habit);

        switchBtn = (Switch)findViewById(R.id.CH_switch);


        switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { //Show quit list

                } else { //Show start list

                }
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.CH_submitBtn:
                startActivity(new Intent(ChooseHabit.this, AddHabit.class));
                break;
        }
    }
}
