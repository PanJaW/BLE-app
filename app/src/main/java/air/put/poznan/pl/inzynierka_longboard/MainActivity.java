package air.put.poznan.pl.inzynierka_longboard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import air.put.poznan.pl.inzynierka_longboard.BLEService.LocalBinder;


public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private List<String> mDevicesList;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private Handler mHandler;
    private ScanCallback mScanCallback;
    private Set<String> mMacAddresses = new HashSet<>();
    boolean mBound = false;
    private BLEService mBleService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList();
            }
        });
        findViewById(R.id.Set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewActivity();
            }
        });

        mListView = findViewById(R.id.found_devices_list);
        mDevicesList = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.devices_list_item, mDevicesList);
        mListView.setAdapter(adapter);


        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // creating a List of found devices
                String mac = result.getDevice().getAddress();
                if (!mMacAddresses.contains(mac)) {
                    mDevicesList.add(mac);
                    adapter.notifyDataSetChanged();
                    mMacAddresses.add(mac);

                    if (mac.equals(getString(R.string.mac_bluetooth_adapter))) {
                        BluetoothDevice mBluetoothDevice = result.getDevice();
                        connectToDevice(mBluetoothDevice);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Toast.makeText(MainActivity.this, "No devices found", Toast.LENGTH_SHORT).show();
            }

        };
        Intent intent = new Intent(this, BLEService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void connectToDevice(BluetoothDevice device) {
        if (mBleService.getmGatt() == null) {
            mBleService.setmGatt(device.connectGatt(this, false, gattCallback));
            scanLeDevice(false);// will stop after first device detection
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(this, "Bluetooth activated", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void refreshList() {
        scanLeDevice(mBluetoothAdapter.isEnabled());
    }

    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            bluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mBleService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    mBleService.getmGatt().discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");

            }
        }

        public void onServicesDiscovered(BluetoothGatt mGatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService gattService : mGatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        Log.i("OnServicesDiscovered", characteristic.getUuid().toString());
                        if (characteristic.getUuid().toString().equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                            mBleService.setmCharacteristic(characteristic);
                            Log.i("OnServicesDiscovered", "TARGET_CHARACTERISTIC_FOUND");
                            startNewActivity();
                        }
                    }
                }
            } else {
                Log.i("OnServicesDiscovered", "NO_GATT_SERVICES_DISCOVERED");
            }

        }
    };



    public void startNewActivity() {
        Intent intent = new Intent(this, SpeedActivity.class);
        startActivity(intent);
    }


}


