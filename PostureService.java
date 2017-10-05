package com.slouchieteam.slouchie;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.hardware.Sensor;

import javax.vecmath.Vector3f;

public class PostureService extends Service {

    //runnable delay to aviod notification and ui lag
    int startuptime = 2000;


    Vibrator vibrator;
    Handler vibrationhandler;
    PowerManager powerManager;
    PowerManager.WakeLock slouchie_wakelock;
    //static service startup distinctions
    public static int intentstartid = 0;
    public static int intentresetid = 8452 + 1;
    public static int intentpauseid = 8452 + 2;
    public static int intentexitid = 8452 + 3;
    public int resettime = 10000; //time delay to allow user to put phone in his pocket
    public int resetsteptime = 1000; //notification update time
    int mesuresoutsiderange = 0; //used to speed up runnable when alerting user and to simulate vibration patterns
    float borderangle = 12; //maximal deviation angle, user configurable
    public static String PACKAGE_NAME;
    Notification posturenotification;
    int countdown;
    NotificationCompat.Action pauseaction;
    Sensor gravitysensor;
    SensorManager gravitysensormanager;
    boolean resettrackingtakemesurement;
    boolean trackingrunning = false;
    boolean pausetracking = false;
    boolean strongvibration = false;
    float angle; //current read angle

    //vectors
    Vector3f gravityreferencevalues = new Vector3f(0, 1, 0);
    Vector3f gravityvalues = new Vector3f(0, 1, 0);


    @Override
    public void onCreate() {

        //SETS UP VARIABLES

        //gets package name
        PACKAGE_NAME = getApplicationContext().getPackageName();

        //vibration
        vibrationhandler = new Handler();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //wakelock
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        slouchie_wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Slouchie_service_wakelock");

        //gravity sensor
        gravitysensormanager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitysensor = gravitysensormanager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravitysensormanager.registerListener(gravitysensorlistener, gravitysensor, SensorManager.SENSOR_DELAY_NORMAL);

        //reading app preferences
        readpreferencesandapply();
        super.onCreate();
    }

    void readpreferencesandapply() {
        borderangle = (float) getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("angle", 12);
        strongvibration = getSharedPreferences(getPackageName(), MODE_PRIVATE).getBoolean("strongvibration", false);
    }

    private void goforeground() {
        //CREATES NOTIFICATION
        //CREATES NOTIFICATION
        Intent openappintent = new Intent(this, MainActivity.class);
        PendingIntent openapppendingIntent = PendingIntent.getActivity(this, 0, openappintent, 0);

        Intent resettrackingserviceintent = new Intent(this, PostureService.class);
        resettrackingserviceintent.putExtra(PACKAGE_NAME + ".myaction", intentresetid);
        PendingIntent resettrackingpendingIntent = PendingIntent.getService(this, 56, resettrackingserviceintent, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent pauseserviceintent = new Intent(this, PostureService.class);
        pauseserviceintent.putExtra(PACKAGE_NAME + ".myaction", intentpauseid);
        PendingIntent pausependingpendingIntent = PendingIntent.getService(this, 43, pauseserviceintent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent exitserviceintent = new Intent(this, PostureService.class);
        exitserviceintent.putExtra(PACKAGE_NAME + ".myaction", intentexitid);
        PendingIntent exitpendingpendingIntent = PendingIntent.getService(this, 54, exitserviceintent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getBaseContext());

        if (!pausetracking) {
            builder.setContentTitle(getString(R.string.slouchierunning));
            pauseaction = new NotificationCompat.Action(R.drawable.ic_pause_circle_outline_black_24dp, getString(R.string.pause), pausependingpendingIntent);
        } else {
            pauseaction = new NotificationCompat.Action(R.drawable.ic_pause_circle_filled_black_24dp, getString(R.string.paused), pausependingpendingIntent);
            builder.setContentTitle(getString(R.string.slouchiepaused));

        }

        builder
                .setColor(Color.rgb(43, 169, 224))
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(openapppendingIntent);
        if (countdown == 0) {
            builder.setPriority(Notification.PRIORITY_DEFAULT);

            builder
                    .addAction(R.drawable.ic_refresh_black_24dp, getString(R.string.reset), resettrackingpendingIntent)
                    .addAction(pauseaction)
                    .addAction(R.drawable.ic_highlight_off_black_24dp, getString(R.string.exit), exitpendingpendingIntent);
        }

        if (countdown > 0 && !pausetracking) {
            builder.setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(new long[50]);
            builder.setContentText(getString(R.string.situpright) + " " + String.valueOf(countdown) + " " + getString(R.string.seconds))
                    .setProgress(resettime / resetsteptime, resettime / resetsteptime - countdown, false)
                    .setContentTitle(getString(R.string.slouchiestarting));
        }


        posturenotification = builder.build();
        startForeground(55, posturenotification);
    }


    @Override
    public void onDestroy() {
        cleanup();

        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//DOES ACTION DEPENDING ON CALL TYPE
        int intentactionflag = 0;

        //gets call type
        if (!(intent == null)) {
            intentactionflag = intent.getIntExtra(PACKAGE_NAME + ".myaction", 0);
        }

        //tracking starting or restarting
        if (intentactionflag == intentstartid || intentactionflag == intentresetid) {
            //  if (intentactionflag == intentstartid) {
            //   }
            begintracking();
        }
        if (intentactionflag == intentpauseid && countdown == 0) {
            pausetracking = !pausetracking;
            if (!pausetracking) {
                begintracking();
            } else {
                endtracking();
            }
        }
        if (intentactionflag == intentexitid) {
            cleanup();
        }
        goforeground();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void cleanup() {

        //CLEANS UP

        //releases wakelock, runnable
        endtracking();

        //sends data to UI
        MainActivity.trackingservicerunning = false;
        MainActivity.servicestopped();

        //stops
        stopSelf();
    }

    public void endtracking() {
        if (slouchie_wakelock.isHeld()) {
            slouchie_wakelock.release();
        }
        //RELEASES WAKELOCK, ENDS VIBRATION RUNNABLE HANDLER
        vibrationhandler.removeCallbacks(vibrationrunnable);
    }

    public void begintracking() {
        pausetracking = false;
        if (!slouchie_wakelock.isHeld()) {
            slouchie_wakelock.acquire();
        }
        vibrationhandler.postDelayed(vibrationrunnable, startuptime);
        trackingrunning = false;
        countdown = resettime / resetsteptime;

        new CountDownTimer(resettime, resetsteptime) {
            @Override
            public void onFinish() {
                MainActivity.trackingservicerunning = true;
                resettrackingtakemesurement = true;
                trackingrunning = true;
                countdown = 0;
                goforeground();
                vibrator.vibrate(100);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                countdown--;
                goforeground();

            }
        }.start();

    }


    SensorEventListener gravitysensorlistener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (trackingrunning && !pausetracking) {
                if (resettrackingtakemesurement) {
                    gravityreferencevalues = new Vector3f(event.values.clone());

                }
                gravityvalues = new Vector3f(event.values.clone());
                angle = (float) (gravityvalues.angle(gravityreferencevalues) / Math.PI * 180);

                //miejsce na logikę usredniajacą

                resettrackingtakemesurement = false;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    Runnable vibrationrunnable = new Runnable() {
        @Override
        public void run() {
            int multiplier = 3;
            if (trackingrunning && !pausetracking) {
                if (angle < borderangle && mesuresoutsiderange > 0) {
                    mesuresoutsiderange = -15;
                }
                if (angle > borderangle) {
                    if (mesuresoutsiderange < 0) {
                        mesuresoutsiderange = 0;
                    }
                    mesuresoutsiderange++;

                    if (mesuresoutsiderange > 0 * multiplier && mesuresoutsiderange < 12 * multiplier) {
                        if ((mesuresoutsiderange - 1) % 3 <= 1) {
                            if (strongvibration) {
                                vibrator.vibrate(300);
                            } else {
                                vibrator.vibrate(200);
                            }

                        }
                    }
                    if (mesuresoutsiderange > 12 * multiplier && mesuresoutsiderange < 43 * multiplier) {
                        if ((mesuresoutsiderange - 1) % 4 <= 1) {
                            if (strongvibration) {
                                vibrator.vibrate(400);
                            } else {
                                vibrator.vibrate(300);
                            }

                        }

                    }
                    if (mesuresoutsiderange > 43 * multiplier && mesuresoutsiderange < 45 * multiplier) {
                        vibrator.vibrate(700);
                    }
                    if (mesuresoutsiderange > 45 * multiplier) {
                        pausetracking = true;
                        goforeground();
                        endtracking();
                        return;
                    }
                }
                if (mesuresoutsiderange < 0) {
                    mesuresoutsiderange++;
                }


            }
            if (mesuresoutsiderange == 0 || pausetracking) {
                vibrationhandler.postDelayed(this, 1300);
            } else {

                vibrationhandler.postDelayed(this, 700);
            }
            readpreferencesandapply();
        }
    };

}
