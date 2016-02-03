package com.example.shruthi.bluetoothsensor;
import android.text.format.DateUtils;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;


public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "Bluetooth Sensor";
    private static final String DEVICE_NAME = "CC2650 SensorTag";
    Button button;
//UUIDs of humidity service and temperature service
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");

    private static final UUID TEMPERATURE_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID TEMPERATURE_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID TEMPERATURE_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private SparseArray<BluetoothDevice> BluetoothDevices;
    private BluetoothGatt ConnectedGatt;
    private BluetoothAdapter BluetoothAdapter;
    public TextView Temperature, Humidity;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);
        button = (Button) findViewById(R.id.Next);
        //  buttonOnClick(button);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.Next);

        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(v.getContext(), MapsActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        Temperature = (TextView) findViewById(R.id.text_temperature);
        Humidity = (TextView) findViewById(R.id.text_humidity);

        TextView textView = (TextView) findViewById(R.id.DATE);
        textView.setText(DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_12HOUR));

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter = manager.getAdapter();
        BluetoothDevices = new SparseArray<BluetoothDevice>();


        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);


    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * To check if bluetooth is enabled
         */
        if (!BluetoothAdapter.isEnabled()) {
            //Enable bluetooth if the bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }
        /*
         * To check whether device supports Bluetooth LE
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        clearValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        BluetoothAdapter.stopLeScan(this);
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };


    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //  Disconnect
        if (ConnectedGatt != null) {
            ConnectedGatt.disconnect();
            ConnectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {

        getMenuInflater().inflate(R.menu.menu_main, m);
        //Add device to menu
        for (int i = 0; i < BluetoothDevices.size(); i++) {
            BluetoothDevice device = BluetoothDevices.valueAt(i);

            m.add(0, BluetoothDevices.keyAt(i), 0, device.getName());


        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                BluetoothDevices.clear();
                startScan();
                return true;
            default:

                //Connect with the scanned device
                BluetoothDevice devices = BluetoothDevices.get(item.getItemId());
                Log.i(TAG, "Connecting to " + devices.getName());
                //connect to device

                ConnectedGatt = devices.connectGatt(this, false, mGattCallback);

                return super.onOptionsItemSelected(item);

        }
    }


    private void clearValues() {
        Temperature.setText("...");
        Humidity.setText("...");

    }

    private void startScan() {
        BluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, 4000);
    }

    private void stopScan() {

        BluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }

    //scan devices
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName());
        //add sensortag to menu

        if (DEVICE_NAME.equals(device.getName())) {

            BluetoothDevices.put(device.hashCode(), device);
            //Update menu
            invalidateOptionsMenu();
        }
    }

    //method to get data
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        private int State = 0;

//enable sensors to read data
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (State) {

                case 0:
                    Log.d(TAG, "Enabling humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_CONFIG_CHAR);
                    characteristic.setValue(new byte[]{0x01});
                    break;
                case 1:
                    Log.d(TAG, "Enabling Temperature");
                    characteristic = gatt.getService(TEMPERATURE_SERVICE)
                            .getCharacteristic(TEMPERATURE_CONFIG_CHAR);
                    characteristic.setValue(new byte[]{0x01});
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            gatt.writeCharacteristic(characteristic);
        }

        //read data from sensor
        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (State) {

                case 0:
                    Log.d(TAG, "Reading humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Reading Temperature");
                    characteristic = gatt.getService(TEMPERATURE_SERVICE)
                            .getCharacteristic(TEMPERATURE_DATA_CHAR);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            gatt.readCharacteristic(characteristic);
        }
//enable notifications from sensor

        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (State) {
                case 0:
                    Log.d(TAG, "Set notify humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Set notify temperature");
                    characteristic = gatt.getService(TEMPERATURE_SERVICE)
                            .getCharacteristic(TEMPERATURE_DATA_CHAR);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }


            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " is " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                //discover services in sensors once connected
                gatt.discoverServices();

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*send clear UI when sensor disconnected
                 */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                //on failure disconnect
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: " + status);
            enableNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //update UI on data read
            if (HUMIDITY_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUM, characteristic));
            }
            if (TEMPERATURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_TEMP, characteristic));
            }
            //enable notification
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //read the initial value after enabling sensor
            readNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*Update UI for each characteristic change*/
            if (HUMIDITY_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUM, characteristic));
            }
            if (TEMPERATURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_TEMP, characteristic));
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //move to the next sensor and start over with enable
            State++;
            enableNextSensor(gatt);
        }


        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };
    // Handler to process event results
    private static final int MSG_HUM = 1;
    private static final int MSG_TEMP = 2;
    private static final int MSG_DISMISS = 3;
    private static final int MSG_CLEAR = 4;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_HUM:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error in extracting humidity data");
                        return;
                    }
                    updateHumidity(characteristic);
                    break;
                case MSG_TEMP:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error in extracting temperature data");
                        return;
                    }
                    updateTemperature(characteristic);
                    break;
                case MSG_DISMISS:
                    progressDialog.hide();
                    break;
                case MSG_CLEAR:
                    clearValues();
                    break;
            }
        }
    };

    /* Method to extract humidity data and update  UI */
    public void updateHumidity(BluetoothGattCharacteristic characteristic) {
        double humidity = DataCalculations.Humidity(characteristic);
        Humidity.setText(String.format("%.0f%%", humidity));
        Toast.makeText(getApplicationContext(), "Humidity is:" + (String.format("%.0f%%", humidity)) + "for current location", Toast.LENGTH_SHORT
        ).show();
    }

    //Method to extract temperature data and update UI
    public void updateTemperature(BluetoothGattCharacteristic characteristic) {
        double temperature = DataCalculations.Temperature(characteristic);
        Temperature.setText(String.format("%.0fC", temperature));
        Toast.makeText(getApplicationContext(), "Temperature is:" + (String.format("%.0fC", temperature)) + "for current location", Toast.LENGTH_SHORT
        ).show();


    }

}

