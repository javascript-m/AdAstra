package com.ad_astra.maja.adastra;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupsFragment extends Fragment {

    TextView title;
    Button addGroup;

    public GroupsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View groupsFragment = inflater.inflate(R.layout.fragment_groups, container, false);

        title = (TextView) groupsFragment.findViewById(R.id.GF_gTitle);
        addGroup = (Button) groupsFragment.findViewById(R.id.GF_addG);

        try {
            GroupButton gBtn = new GroupButton();

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.GF_holder, gBtn);
            fragmentTransaction.commit();
        } catch (Exception e) {
            title.setText(e.toString());
        }

        addGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ExploreGroups.class));
            }
        });

        // Inflate the layout for this fragment
        return groupsFragment;
    }


}
