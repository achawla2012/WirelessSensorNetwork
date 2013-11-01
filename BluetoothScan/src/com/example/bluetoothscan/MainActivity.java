package com.example.bluetoothscan;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.Menu;

public class MainActivity extends Activity {
	
	ListView listDevicesFound;
	TextView stateBluetooth, textScanDevice;
	
	BluetoothAdapter bluetoothAdapter;
	ArrayAdapter<String> btArrayAdapter;
	ArrayList<BluetoothDevice> arrayListBluetoothDevices;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        
		textScanDevice = (TextView)findViewById(R.id.scanMsg);     
        stateBluetooth = (TextView)findViewById(R.id.bluetoothstate);        
        listDevicesFound = (ListView)findViewById(R.id.devicesfound);
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        listDevicesFound.setAdapter(btArrayAdapter);

        arrayListBluetoothDevices = null;
        arrayListBluetoothDevices = new ArrayList<BluetoothDevice>();

    	if (bluetoothAdapter == null){
        	stateBluetooth.setText("No Bluetooth support on this device ! ");
    	}
        if (bluetoothAdapter!= null  && bluetoothAdapter.isDiscovering()){
    		bluetoothAdapter.cancelDiscovery();
        }
    	
        if (bluetoothAdapter.isEnabled()){
        	if(bluetoothAdapter.isDiscovering()){
        			stateBluetooth.setText("Bluetooth in device discovery mode.");
        	}else{
        		stateBluetooth.setText("Bluetooth is ON.");
        		Log.i("CheckBlueToothState()","In initializing state");
        		textScanDevice.setText("Scanning in progress...");
        		bluetoothAdapter.startDiscovery();
        		}
        }else{
        		stateBluetooth.setText("Bluetooth is OFF");
        		bluetoothAdapter.enable();
       	}

        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
    	if (bluetoothAdapter.isDiscovering()){
    		bluetoothAdapter.cancelDiscovery();
        }
    	if(bluetoothAdapter!=null){
    		bluetoothAdapter.disable();
    	}
		super.onDestroy();
		unregisterReceiver(ActionFoundReceiver);
	}

	
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            if(arrayListBluetoothDevices.size() < 1	){
	            	btArrayAdapter.add(device.getName()+"\n"+device.getAddress());
	            	arrayListBluetoothDevices.add(device);
	            	btArrayAdapter.notifyDataSetChanged();
	            }
	            else{
	            	boolean flag = true;
	            	for(int i = 0 ; i < arrayListBluetoothDevices.size() ; i++){
	                        if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress()))
	                        {
	                            flag = false;
	                        }
	                }
	                if(flag == true){
	                	btArrayAdapter.add(device.getName()+"\n"+device.getAddress());
	                    arrayListBluetoothDevices.add(device);
	                    btArrayAdapter.notifyDataSetChanged();
	                }
	            }
		    }
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
    			textScanDevice.setText("Scanning done.");
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
    			textScanDevice.setText("Scanning in progress...");
			}
			else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
	            switch (state) {
	            case BluetoothAdapter.STATE_OFF:
	            	stateBluetooth.setText("Bluetooth is OFF");
	                break;
	            case BluetoothAdapter.STATE_TURNING_OFF:
	            	stateBluetooth.setText("Turning Bluetooth off...");
	                break;
	            case BluetoothAdapter.STATE_ON:
	            	stateBluetooth.setText("Bluetooth is ON");
        			bluetoothAdapter.startDiscovery();
	                break;
	            case BluetoothAdapter.STATE_TURNING_ON:
	            	stateBluetooth.setText("Turning Bluetooth on...");
	                break;
	            }
	        }
			
		}
    };
}

	