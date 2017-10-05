package com.slouchieteam.slouchie;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {
    public static ToggleButton servicetogglebutton;
    int countdown = 0;
    public static boolean trackingservicerunning;
    Intent postureserviceintent;

    ToggleButton.OnCheckedChangeListener changeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                beginPostureTracking();
            } else {
                stopService(postureserviceintent);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        servicetogglebutton = (ToggleButton) findViewById(R.id.servicetogglebutton);

        if (trackingservicerunning) {
            servicetogglebutton.setChecked(true);
        } else {
            servicetogglebutton.setChecked(false);
        }
        servicetogglebutton.setOnCheckedChangeListener(changeListener);
        View scrollview = findViewById(R.id.scrollView);
        scrollview.setVisibility(View.INVISIBLE);
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slidein);
                View scrollview = findViewById(R.id.scrollView);
                scrollview.startAnimation(anim);
                scrollview.setVisibility(View.VISIBLE);
            }
        });
        postureserviceintent = new Intent(this, PostureService.class);
        postureserviceintent.putExtra(PostureService.PACKAGE_NAME + ".myaction", PostureService.intentstartid);
        makecardshideable();

        int angle = getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("angle", 12);
        getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putInt("angle", angle).commit();
        boolean strongvibration = getSharedPreferences(getPackageName(), MODE_PRIVATE).getBoolean("strongvibration", false);
        getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("strongvibration", strongvibration).commit();
        ((TextView) findViewById(R.id.seektext)).setText(String.valueOf(angle));
        SeekBar bar = ((SeekBar) findViewById(R.id.seekbar));
        bar.setOnSeekBarChangeListener(seekbarlistener);
        bar.setProgress(angle);
        SwitchCompat vibrationswitch = (SwitchCompat) findViewById(R.id.vibrationswitch);
        vibrationswitch.setChecked(strongvibration);
        vibrationswitch.setOnCheckedChangeListener(vibrationswitchchangedlistener);
        LinearLayout vibrationswitchlayout = (LinearLayout) findViewById(R.id.vibrationswitchlayout);
        vibrationswitchlayout.setOnClickListener(switchlayoutlistener);
    }

    public void makecardshideable() {
        LinearLayout Cardslayout = (LinearLayout) findViewById(R.id.cardslayout);
        if (Cardslayout != null) {
            int count = Cardslayout.getChildCount();

            for (int i = 0; i <= count; i++) {
                View v = Cardslayout.getChildAt(i);
                if (v instanceof CardView) {
                    v.setOnClickListener(hidecardslistener);
                }
            }
        }

    }

    public static void servicestopped() {
        servicetogglebutton.setChecked(false);
    }

    void beginPostureTracking() {

        //   stopService(postureserviceintent);
        startService(postureserviceintent);

    }

    View.OnClickListener switchlayoutlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SwitchCompat vibrationswitch = (SwitchCompat) findViewById(R.id.vibrationswitch);
            vibrationswitch.setChecked(!vibrationswitch.isChecked());
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("strongvibration", vibrationswitch.isChecked()).commit();
        }
    };

    SwitchCompat.OnCheckedChangeListener vibrationswitchchangedlistener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("strongvibration", isChecked).commit();
        }
    };

    SeekBar.OnSeekBarChangeListener seekbarlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            ((TextView) findViewById(R.id.seektext)).setText(String.valueOf(progress) + "°");
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putInt("angle", progress).commit();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    View.OnClickListener hidecardslistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final View tobehidden = v.findViewWithTag("hideable");
            if (tobehidden != null) {
                if (tobehidden.getVisibility() == View.GONE) {


                    tobehidden.post(new Runnable() {
                        @Override
                        public void run() {
                            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.appear);

                            tobehidden.startAnimation(anim);
                            tobehidden.setVisibility(View.VISIBLE);
                        }
                    });


                } else {


                    tobehidden.post(new Runnable() {
                        @Override
                        public void run() {
                            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.disappear);

                            tobehidden.startAnimation(anim);
                        }
                    });
                    tobehidden.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tobehidden.setVisibility(View.GONE);
                        }
                    }, 300);
                }
            }


        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //   getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        //   vibratehandler.removeCallbacks(vibraterunnable);
        super.onDestroy();
    }


}


