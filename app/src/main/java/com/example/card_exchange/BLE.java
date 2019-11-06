package com.example.card_exchange;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static android.R.layout.simple_list_item_1;
import static com.example.card_exchange.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;
import static com.example.card_exchange.Constants.CARD_EXCHANGE_SERVICE_UUID;
import static com.example.card_exchange.MainActivity.Input_Company;
import static com.example.card_exchange.MainActivity.Input_Name;
import static com.example.card_exchange.MainActivity.Input_Phone;

// TODO 相同名片不能儲存兩次

public class BLE extends BluetoothActivity implements View.OnClickListener {

     
    private Button btn_Back;
    private Switch switch_adv;
    private BluetoothGattService mSampleService;
    private BluetoothGattCharacteristic mSampleCharacteristic;
    private BluetoothGattServer mGattServer;
    private HashSet<BluetoothDevice> mBluetoothDevices;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;

    Button saved_card;
    Button startScanningButton;
    Button stopScanningButton;
    Button change_to_list;
    ListView peripheralListView;
    String uuid_text;
    String uuid;
    private List<String> list_device;
    private  List<String> list_device_detail;
    private List<String> save_list;
    private List<String> save_name_list;
    ArrayList<String> name_list = new ArrayList<>();
    private ArrayAdapter adapter;
    String list;
    String name;


    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
//    String[] name_list = new String[]{
//            "alice",
//            "jane",
//            "lucy",
//            "robin",
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);



        if(!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()){
            Toast.makeText(this,"Advertisement 不支援",Toast.LENGTH_SHORT).show();
        }
        switch_adv = (Switch)findViewById(R.id.switch_adv);
        btn_Back = (Button)findViewById(R.id.btn_Back);
        btn_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(BLE.this,MainActivity.class);
                startActivity(intent);
                BLE.this.finish();
            }
        });
        switch_adv.setOnClickListener(this);
        setGattServer();
        setBluetoothService();

        list_device = new ArrayList<>();
        list_device_detail = new ArrayList<>();
        save_list = new ArrayList<>();
        save_name_list = new ArrayList<>();

        peripheralListView = (ListView) findViewById(R.id.PeripheralListView);
        adapter = new ArrayAdapter<>(this , android.R.layout.simple_list_item_1 ,name_list);
        peripheralListView.setAdapter(adapter);

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });
        saved_card = findViewById(R.id.saved_card);
        saved_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(BLE.this)
                        .setItems(save_name_list.toArray(new String[save_name_list.size()]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(BLE.this,save_list.get(which), Toast.LENGTH_SHORT).show();
                                //Toast.makeText(getApplicationContext(), (save_list.get(which)) , Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        });


        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);



        //選擇filter
        final Spinner uuid_spinner = (Spinner)findViewById(R.id.uuid_spinner);
        ArrayAdapter<CharSequence> nAdapter = ArrayAdapter.createFromResource(
                this, R.array.uuid_spinner, android.R.layout.simple_spinner_item );
        nAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        uuid_spinner.setAdapter(nAdapter);
        uuid_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                uuid_text = uuid_spinner.getSelectedItem().toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        add_original_data();

        name_list.add("alice");
        name_list.add("jane");
        name_list.add("lucy");
        name_list.add("robin");

        peripheralListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                name = name_list.get(i);
                list = list_device_detail.get(i);
                new AlertDialog.Builder(BLE.this)
                        .setMessage(list)
                        .setPositiveButton("save",new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                save_list.add(list);
                                save_name_list.add(name);
                                Log.d(MainActivity.TAG,"save_name_list: "+save_name_list);
                                Log.d(MainActivity.TAG,"save_list: "+save_list);
                            }
                        })
                        .setNegativeButton("leave", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();

                Log.d(MainActivity.TAG,"list: "+list +"i: " + i);
                //Toast.makeText(BLE.this,list, Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void startAdvertising() {
        // TODO bluetooth - maybe bindService? what happens when closing app?
        startService(getServiceIntent(this));
    }

    private void stopAdvertising() {
        stopService(getServiceIntent(this));
//        mEnableAdvertisementSwitch.setChecked(false);
    }

    private void setGattServer() {

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        } else {
            showMsgText("error unknown");
        }
    }

    private void setBluetoothService() {

        // create the Service
        mSampleService = new BluetoothGattService(CARD_EXCHANGE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        /*
        create the Characteristic.
        we need to grant to the Client permission to read (for when the user clicks the "Request Characteristic" button).
        no need for notify permission as this is an action the Server initiate.
         */
        //mSampleCharacteristic = new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //setCharacteristic(); // set initial state

        // add the Characteristic to the Service
        //mSampleService.addCharacteristic(mSampleCharacteristic);

        // add the Service to the Server/Peripheral
        if (mGattServer != null) {
            mGattServer.addService(mSampleService);
        }
    }


    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {

            super.onConnectionStateChange(device, status, newState);

            String msg;

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothGatt.STATE_CONNECTED) {

                    mBluetoothDevices.add(device);

                    msg = "Connected to device: " + device.getAddress();
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {

                    mBluetoothDevices.remove(device);

                    msg = "Disconnected from device";
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);
                }

            } else {
                mBluetoothDevices.remove(device);

                msg = getString(R.string.status_error_when_connecting) + ": " + status;
                Log.e(MainActivity.TAG, msg);
                showMsgText(msg);

            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(MainActivity.TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (mGattServer == null) {
                return;
            }

            Log.d(MainActivity.TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(characteristic.getValue()));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

//        @Override
//        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//
//            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
//
//            Log.v(MainActivity.TAG, "Characteristic Write request: " + Arrays.toString(value));
//
//            mSampleCharacteristic.setValue(value);
//
//            if (responseNeeded) {
//                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
//            }
//
//        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            if (mGattServer == null) {
                return;
            }

            Log.d(MainActivity.TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(descriptor.getValue()));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {

            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            Log.v(MainActivity.TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                } else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                } else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                } else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                } else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            } else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
//            if (responseNeeded) {
//                mGattServer.sendResponse(device, requestId, status,
//            /* No need to respond with offset */ 0,
//            /* No need to respond with a value */ null);
//            }

        }
    };

    private Intent getServiceIntent(Context context) {
        return new Intent(context, PeripheralAdvertiseService.class);
    }
    @Override
    public void onClick(View view) {

        switch(view.getId()) {
            case R.id.switch_adv:
                if(!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
                    Toast.makeText(this, "Advertisement 不支援", Toast.LENGTH_SHORT).show();
                    switch_adv.setChecked(false);
                }else {
                    Switch switchToggle = (Switch) view;
                    if (switchToggle.isChecked()) {
                        startAdvertising();
                        Log.d(MainActivity.TAG, "on");

                    } else {
                        stopAdvertising();
                        Log.d(MainActivity.TAG, "off");
                    }
                    break;
                }
        }

        if(!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()){
            Toast.makeText(this,"Advertisement 不支援",Toast.LENGTH_SHORT).show();
        }




    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ble;
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        //String[] strArray = new String[] {output.toString()};
        return output.toString();
    }

    public static String byte2HexStr(byte[] b) {
        String stmp="";
        StringBuilder sb = new StringBuilder("");
        for (int n=0;n<b.length;n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length()==1)? "0"+stmp : stmp);
        }
        //String[] strArray = new String[] {sb.toString().toUpperCase().trim()};
        return sb.toString().trim();
        //trim:去掉前後空格 ； toUpperCase:變成大寫
    }


    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(MainActivity.TAG,"onScanResult");
            byte[] byte_1 = result.getScanRecord().getServiceData(ParcelUuid.fromString(CARD_EXCHANGE_SERVICE_UUID.toString()));

            String data_16 = byte2HexStr(byte_1);
            String[] data_split = data_16.split("2b");
            Log.d(MainActivity.TAG,"data_split1:"+data_split[0]);
            Log.d(MainActivity.TAG,"data_split2:"+data_split[1]);
            Log.d(MainActivity.TAG,"data_split3:"+data_split[2]);
            String data_ascii_name = hexToAscii(data_split[0]);
            String data_ascii_company = hexToAscii(data_split[1]);
            String data_ascii_phone = hexToAscii(data_split[2]);
            //int data_ascii_phone = byteArrayToInt(data_split[2].getBytes());

            add_name_list(data_ascii_name);

            String msg;
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            msg=      "name(ascii): "+ data_ascii_name +"\n"
                    + "company(ascii): " + data_ascii_company +"\n"
                    + "phone: " + data_ascii_phone + "\n"
                    + "rssi: " + result.getRssi() +"\n"
                    + "add: " + result.getDevice().getAddress() +"\n"
                    + "service uuid: " + result.getScanRecord().getServiceUuids() +"\n" + "time: " + currentTime +"\n";

            String msg_display = "name: "+ data_ascii_name +"\n"
                    + "company: " + data_ascii_company +"\n"
                    + "phone: " + data_ascii_phone + "\n";

            if(!list_device.contains(result.getDevice().getAddress())){
                list_device.add(result.getDevice().getAddress());
                list_device_detail.add(msg_display);
            }

            if(list_device.contains(result.getDevice().getAddress())){
                list_device.set(list_device.indexOf(result.getDevice().getAddress()),result.getDevice().getAddress());
                list_device_detail.set(list_device.indexOf(result.getDevice().getAddress()),msg_display);
            }
            Log.d(MainActivity.TAG, "list_device_detail"+list_device_detail.get(0));

            list_device.add(data_ascii_name);

            Log.d(MainActivity.TAG, Arrays.toString(list_device.toArray()));
            Log.d(MainActivity.TAG,msg);

//            peripheralTextView.append(msg);
//            // auto scroll for text view
//            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
//            // if there is no need to scroll, scrollAmount will be <=0
//            if (scrollAmount > 0)
//                peripheralTextView.scrollTo(0, scrollAmount);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        Log.d(MainActivity.TAG,"startScanning");
        //list_device.clear();

        System.out.println("start scanning");
        //peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        scan_filter();
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                switch (uuid_text){
//                    case "none":
//                        mBluetoothScanner.startScan(leScanCallback);
//                        break;
//                    case "find me":
//                        uuid = "00001802-0000-1000-8000-00805f9b34fb";
//                        scan_filter();
//                        break;
//                    case "blood pressure":
//                        uuid = "00001810-0000-1000-8000-00805f9b34fb";
//                        scan_filter();
//                        break;
//                    case "alert notification":
//                        uuid = "00001811-0000-1000-8000-00805f9b34fb";
//                        scan_filter();
//                        break;
//                    case"heart rate":
//                        uuid = "0000180d-0000-1000-8000-00805f9b34fb";
//                        scan_filter();
//                        break;
//                    case "time":
//                        uuid = "00001807-0000-1000-8000-00805f9b34fb";
//                        scan_filter();
//                        break;
//                    default: //Oh no, it's working day
//                        //This code is executed when value of variable 'day'
//                        //doesn't match with any of case above
//                        break;
//                }
//            }
//        });
    }

    public void stopScanning() {
        Log.d(MainActivity.TAG,"stopScanning");
        System.out.println("stopping scanning");
        //peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBluetoothScanner.stopScan(leScanCallback);
            }
        });
    }

    public void scan_filter(){
        ScanFilter beaconFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(Constants.CARD_EXCHANGE_SERVICE_UUID.toString())).build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(beaconFilter);
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mBluetoothScanner.startScan(filters, settings, leScanCallback);
        Log.d(MainActivity.TAG,"scan_filter");
    }

    public void add_name_list(String new_name){
        Log.d(MainActivity.TAG,"new_name: "+new_name);
        if(name_list.contains(new_name)!=true){
            name_list.add(new_name);
            adapter.notifyDataSetChanged();
        }
    }

    public void add_original_data(){

        list_device_detail.add("NAME:    alice"+"\n"
                              +"COMPANY: ncu"+"\n"
                              +"PHONE:   0956882845"+"\n");
        list_device_detail.add("NAME:    jane"+"\n"
                +"COMPANY: nctu"+"\n"
                +"PHONE:   0924056987"+"\n");
        list_device_detail.add("NAME:    lucy"+"\n"
                +"COMPANY: ncu"+"\n"
                +"PHONE:   0983161879"+"\n");
        list_device_detail.add("NAME:    robin"+"\n"
                +"COMPANY: nthu"+"\n"
                +"PHONE:   0937176589"+"\n");
    }
}
