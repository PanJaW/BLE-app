package air.put.poznan.pl.inzynierka_longboard;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


public class BLEService extends Service {
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mGatt;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BLEService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public BluetoothGattCharacteristic getmCharacteristic() {
        return mCharacteristic;
    }

    public void setmCharacteristic(BluetoothGattCharacteristic mCharacteristic) {
        this.mCharacteristic = mCharacteristic;
    }

    public BluetoothDevice getmBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setmBluetoothDevice(BluetoothDevice mBluetoothDevice) {
        this.mBluetoothDevice = mBluetoothDevice;
    }

    public BluetoothGatt getmGatt() {
        return mGatt;
    }

    public void setmGatt(BluetoothGatt mGatt) {
        this.mGatt = mGatt;
    }
}
