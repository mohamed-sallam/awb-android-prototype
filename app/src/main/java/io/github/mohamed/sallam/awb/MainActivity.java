// Source of Overlay functionality: https://www.geeksforgeeks.org/how-to-draw-over-other-apps-in-android/
package io.github.mohamed.sallam.awb;

import android.app.AppOpsManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.time.Duration;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    private static final int REQ_CODE = 592;
    Button lockBtn;
    Button whitelistBtn;
    NumberPicker secPicker;
    NumberPicker minPicker;
    NumberPicker hrPicker;
    Duration duration = Duration.ZERO;
    ForegroundService mService;
    boolean mBound = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        Long durationFromSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this).getLong("remaining_time", 0L);
        checkOverlayPermission();
        startService();
        checkGetUsageStatePermission(this);

        secPicker = findViewById(R.id.secPicker);
        secPicker.setMinValue(0);
        secPicker.setMaxValue(59);
        secPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                duration = duration.minusSeconds(oldVal);
                duration = duration.plusSeconds(newVal);
            }
        });

        minPicker = findViewById(R.id.minPicker);
        minPicker.setMinValue(0);
        minPicker.setMaxValue(59);
        minPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                duration = duration.minusMinutes(oldVal);
                duration = duration.plusMinutes(newVal);
            }
        });

        hrPicker = findViewById(R.id.hrPicker);
        hrPicker.setMinValue(0);
        hrPicker.setMaxValue(24);
        hrPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                duration = duration.minusHours(oldVal);
                duration = duration.plusHours(newVal);
            }
        });

        lockBtn = findViewById(R.id.lock_btn);
        lockBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {
                long durationInMillis = duration.toMillis();
                if (mBound && (durationInMillis <= 89999000) && (durationInMillis >= 1) && (durationFromSharedPreferences == 0L)) {
                    makeAwbHomeApp();
                    mService.openOverlayActivity(durationInMillis);
                }
            }

        });

        whitelistBtn = findViewById(R.id.whitelist_btn);
        whitelistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Whitelist.class));
            }
        });
    }

    // Source: https://stackoverflow.com/a/28921586
    public static void checkGetUsageStatePermission(Context context){
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        if(mode != AppOpsManager.MODE_ALLOWED)
            context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    public void makeAwbHomeApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = null;
            roleManager = getSystemService(RoleManager.class);
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                Intent roleRequestIntent = roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_HOME);
                startActivityForResult(roleRequestIntent, REQ_CODE);
            }
        } else {
            Intent selector = new Intent(Intent.ACTION_MAIN);
            selector.addCategory(Intent.CATEGORY_HOME);
            selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(selector); // TODO: maybe we wanna use `startActivityForResult`
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE && resultCode==RESULT_CANCELED) {
            mService.countDownActivity.close();
        }
    }

    // method for starting the service
    public void startService(){
        Intent intent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.canDrawOverlays(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }else{
            startService(intent);
        }
    }

    // method to ask user to grant the Overlay permission
    public void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(myIntent);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ForegroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            ForegroundService.ForegroundServiceBinder binder = (ForegroundService.ForegroundServiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}