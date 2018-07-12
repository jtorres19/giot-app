package com.giot.tectronix.giot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.giot.tectronix.giot.R;
import com.giot.tectronix.giot.fragments.BarcodeFragment;
import com.giot.tectronix.giot.fragments.BluetoothFragment;
import com.giot.tectronix.giot.fragments.ProfileFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_INTENT_USERNAME = "username";
    public final static String EXTRA_INTENT_EMAIL = "email";

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Intent i = getIntent();
        String username = i.getExtras().getString(EXTRA_INTENT_USERNAME);
        String email = i.getExtras().getString(EXTRA_INTENT_EMAIL);

        AppCompatTextView lblUser = findViewById(R.id.lblUser);
        AppCompatTextView lblEmail = findViewById(R.id.lblEmail);*/

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_reorder_black_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        /*Toast.makeText(this,username + " " + email, Toast.LENGTH_LONG).show();*/

        /*lblUser.setText(username);
        lblEmail.setText(email);*/

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.menu_barcode:
                        cambiarFragment(new BarcodeFragment(),menuItem.getTitle().toString());
                        menuItem.setChecked(true);
                        //bottomNavigationView.setSelectedItemId(menuItem.getItemId());
                        break;

                    case R.id.menu_bluetooth:
                        cambiarFragment(new BluetoothFragment(),menuItem.getTitle().toString());
                        menuItem.setChecked(true);
                        //bottomNavigationView.setSelectedItemId(menuItem.getItemId());
                        break;

                    case R.id.menu_account:
                        cambiarFragment(new ProfileFragment(),menuItem.getTitle().toString());
                        menuItem.setChecked(true);
                        //bottomNavigationView.setSelectedItemId(menuItem.getItemId());
                        break;

                    case R.id.menu_historic:

                        //bottomNavigationView.setSelectedItemId(menuItem.getItemId());
                        break;

                    case  R.id.menu_about:

                        //bottomNavigationView.setSelectedItemId(menuItem.getItemId());
                        break;

                    case R.id.menu_logout:
                        finish();
                }

                //drawerLayout.closeDrawer(GravityCompat.START);
                drawerLayout.closeDrawers();

                return true;
            }
        });

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout, toolbar,R.string.openDrawer,R.string.closeDrawer);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        /*bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.action_barcode:
                        cambiarFragment(new BarcodeFragment(),menuItem.getTitle().toString());
                        break;
                    case R.id.action_bluetooth:
                        cambiarFragment(new BluetoothFragment(),menuItem.getTitle().toString());
                        break;
                    case R.id.action_profile:
                        cambiarFragment(new ProfileFragment(),menuItem.getTitle().toString());
                        break;
                }

                return true;
            }
        });*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cambiarFragment(Fragment f, String title) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, f).commit();
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }else{
            super.onBackPressed();
        }
    }


}
