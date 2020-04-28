package air.put.poznan.pl.inzynierka_longboard;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.anastr.speedviewlib.SpeedView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;

public class SpeedActivity extends AppCompatActivity {

    private BLEService mBleService;
    private boolean mBound = false;
    private int mSpeed = 0;
    private ProgressBar mProgressBar;
    private SpeedView mSpeedView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_activity);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setMax(178);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        mSpeedView = findViewById(R.id.speedView);

        mSpeedView.setMinSpeed(0);
        mSpeedView.setMaxSpeed(50);

    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            mBleService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        TextView mCurrentSpeed = this.findViewById(R.id.CurrentSpeed);



        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:

                if (action == KeyEvent.ACTION_DOWN) {
                    if (mSpeed == 178) {
                        mBleService.getmCharacteristic().setValue(mSpeed, FORMAT_SINT8, 0);
                        writeCharacteristic(mBleService.getmCharacteristic());
                    } else {
                        mSpeed++;
                        mBleService.getmCharacteristic().setValue(mSpeed, FORMAT_SINT8, 0);
                        writeCharacteristic(mBleService.getmCharacteristic());
                    }
                    float mSpeed1 = (mSpeed/255f)*100;
                    float mSpeed2 = BigDecimal.valueOf(mSpeed1)
                            .setScale(2, RoundingMode.HALF_UP)
                            .floatValue();
                    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                    dfs.setDecimalSeparator('.');
                    DecimalFormat df = new DecimalFormat("00.00", dfs);
                    String sSpeed = df.format(mSpeed2);
                    mCurrentSpeed.setText(sSpeed+ " %");

                }
                mProgressBar.setProgress(mSpeed);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {

                    if (mSpeed == 0) {
                        mBleService.getmCharacteristic().setValue(mSpeed, FORMAT_SINT8, 0);
                        writeCharacteristic(mBleService.getmCharacteristic());
                    } else {
                        mSpeed--;
                        mBleService.getmCharacteristic().setValue(mSpeed, FORMAT_SINT8, 0);
                        writeCharacteristic(mBleService.getmCharacteristic());
                    }

                }
                float mSpeed1 = (mSpeed/255f)*100;
                float mSpeed2 = BigDecimal.valueOf(mSpeed1)
                        .setScale(2, RoundingMode.HALF_UP)
                        .floatValue();
                DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                dfs.setDecimalSeparator('.');
                DecimalFormat df = new DecimalFormat("00.00", dfs);
                String sSpeed = df.format(mSpeed2);
                mCurrentSpeed.setText(sSpeed+ " %");
                mProgressBar.setProgress(mSpeed);
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBleService.getmGatt() == null) {
            return;
        }
        mBleService.getmGatt().writeCharacteristic(characteristic);
    }

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            updateSpeed(location);

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };


    void updateSpeed(Location location) {
        float nSpeed = location.getSpeed() * 3.6f;
        float nSpeed1 = BigDecimal.valueOf(nSpeed)
                .setScale(2, RoundingMode.DOWN)
                .floatValue();
        mSpeedView.speedTo(nSpeed1);
    }


}
