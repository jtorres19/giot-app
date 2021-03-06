package com.giot.tectronix.giot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.giot.tectronix.giot.GiotApp;
import com.giot.tectronix.giot.R;
import com.giot.tectronix.giot.fragments.BarcodeFragment;
import com.giot.tectronix.giot.fragments.BluetoothFragment;
import com.giot.tectronix.giot.fragments.ProfileFragment;
import com.giot.tectronix.giot.model.User;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private int cont = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);

        AppCompatTextView lblUser = findViewById(R.id.lblUser);
        AppCompatTextView lblEmail = findViewById(R.id.lblEmail);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.nav_menu);
        //getSupportActionBar().setIcon(getDrawable(R.drawable.logo_giot));

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.tab_bottom);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout, toolbar,R.string.openDrawer,R.string.closeDrawer);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.menu_barcode:
                        navigationView.setCheckedItem(menuItem.getItemId());
                        bottomNavigationView.setSelectedItemId(R.id.action_barcode);
                        cambiarFragment(new BarcodeFragment(),menuItem.getTitle().toString());
                        break;

                    case R.id.menu_bluetooth:
                        navigationView.setCheckedItem(menuItem.getItemId());
                        bottomNavigationView.setSelectedItemId(R.id.action_bluetooth);
                        cambiarFragment(new BluetoothFragment(),menuItem.getTitle().toString());
                        break;

                    case R.id.menu_account:
                        navigationView.setCheckedItem(menuItem.getItemId());
                        bottomNavigationView.setSelectedItemId(R.id.action_profile);
                        cambiarFragment(new ProfileFragment(),menuItem.getTitle().toString());
                        break;

                    case R.id.menu_historic:

                        break;

                    case  R.id.menu_about:

                        break;

                    case R.id.menu_logout:
                        finish();
                }

                drawerLayout.closeDrawer(GravityCompat.START);

                return true;
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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
        });

        cambiarFragment(new BluetoothFragment(),getTitle().toString());

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
        }else if (cont == 0){
            Toast.makeText(getApplicationContext(),"Presione de nuevo para salir",Toast.LENGTH_LONG).show();
            cont++;
        }else {
            //super.onBackPressed();

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }


        new CountDownTimer(3000,1000){

            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                cont = 0;
            }
        }.start();
    }

}
