package com.example.bluetoothscan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

@SuppressWarnings("unused")
public class MainActivity extends Activity {
	
	ListView listviewDevicesFound;
	TextView stateBluetooth, textScanDevice;
	Button send;
	private Thread worker; 
	BluetoothAdapter bluetoothAdapter;
	BluetoothDevice bluetoothDevice, pairedBluetoothDevice;
    BluetoothClass bluetoothClass;
	ArrayAdapter<String> discoveredArrayAdapter,pairedAdapter;
	ArrayList<BluetoothDevice> arrayListDiscoveredDevices,arraylistviewPairedDevices;
	private BluetoothAdapter _bluetoothAdapter;
	private BluetoothSocket _socket;
	private BluetoothDevice _device = null;
	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	private Thread _workerThread;
	private byte[] _readBuffer;
	private int _readBufferPosition;
	private volatile boolean _stopWorker;
	
	private final static char[] HEX_DIGITS =  { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	ListItemClicked listItemClicked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        
		textScanDevice = (TextView)findViewById(R.id.scanMsg);     
        stateBluetooth = (TextView)findViewById(R.id.bluetoothstate); 
        listviewDevicesFound = (ListView)findViewById(R.id.devicesfound);
        send = (Button)findViewById(R.id.sendMsg);
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        discoveredArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        pairedAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_2);
        arrayListDiscoveredDevices = null;
        arrayListDiscoveredDevices = new ArrayList<BluetoothDevice>();
        arraylistviewPairedDevices = null;
        arraylistviewPairedDevices = new ArrayList<BluetoothDevice>();
        
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
        		textScanDevice.setText("Scanning in progress...");
        		bluetoothAdapter.startDiscovery();
        		}
        }else{
        		stateBluetooth.setText("Turning Bluetooth on...");
        		bluetoothAdapter.enable();
       	}

        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        
        pairedAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_2);
        pairedAdapter.notifyDataSetChanged();
        
        listviewDevicesFound.setAdapter(discoveredArrayAdapter);
        discoveredArrayAdapter.notifyDataSetChanged();

        listItemClicked = new ListItemClicked();
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
					sendMessage(v);
				} catch (IOException e) {
					Log.i("Error in sending", " senddd..");
				}
            }
        });
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void getPairedDevice(BluetoothDevice device ){
    	boolean flag = true;
    	for(int i = 0 ; i < arraylistviewPairedDevices.size() ; i++){
                if(device.getAddress().equals(arrayListDiscoveredDevices.get(i).getAddress()))
                {
                    flag = false;
                }
        }
        if(flag == true){
        	pairedAdapter.add(device.getName());
            arrayListDiscoveredDevices.add(device);
            pairedAdapter.notifyDataSetChanged();
            arrayListDiscoveredDevices.get(0).getBondState();
        }
	}
	
	protected void onStart() {
        super.onStart();
        listviewDevicesFound.setOnItemClickListener(listItemClicked);
	}
	
	class ListItemClicked implements OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            bluetoothDevice = arrayListDiscoveredDevices.get(position) ;
            Log.i("Log in onItemClick", "The device : "+bluetoothDevice.toString());
            Boolean isBonded = false;
            try {
                isBonded = createBond(bluetoothDevice);
                if(isBonded){
                	runOnUiThread(new Runnable() {
                        public void run(){
                        	while (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED);
                            ((TextView)view).setText(bluetoothDevice.getName()+ "\nPaired");
    	                	if(bluetoothDevice.getName().startsWith("AMAR")){
    	                        pairedBluetoothDevice = bluetoothDevice;
    	                        send.setEnabled(true);
    	                    }

                        }
                	});
                }
            } catch (Exception e) {
                e.printStackTrace(); 
            }
            Log.i("Log", "The bond is created: "+isBonded);
        }       
    }

    public boolean createBond(BluetoothDevice btDevice) throws Exception  
    { 
        Class<?> class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");  
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);  
        return returnValue.booleanValue();  
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
	            if(arrayListDiscoveredDevices.size() < 1){
			    	switch (device.getBondState() ) {
                	case BluetoothDevice.BOND_BONDED:
                		Log.i("Paired with " , device.getName());
	                	discoveredArrayAdapter.add(device.getName() + "\nPaired");
	                	if(device.getName().startsWith("AMAR")){
	                        pairedBluetoothDevice = device;
	                        send.setEnabled(true);
	                    }
                		break;
                	case BluetoothDevice.BOND_NONE:
                		Log.i("Unpaired with " , device.getName());
	                	discoveredArrayAdapter.add(device.getName() + "\nPair with this device");
	                	break;
	                default:
	                	discoveredArrayAdapter.add(device.getName());
			    	}
	            	arrayListDiscoveredDevices.add(device);
	            	discoveredArrayAdapter.notifyDataSetChanged();
	            }
	            
	            
	            else{
	            	boolean flag = true;
	            	for(int i = 0 ; i < arrayListDiscoveredDevices.size() ; i++){
	                        if(device.getAddress().equals(arrayListDiscoveredDevices.get(i).getAddress()))
	                        {
	                            flag = false;
	                        }
	                }
	                if(flag == true){
	    		    	switch (device.getBondState() ) {
	                	case BluetoothDevice.BOND_BONDED:
	                		Log.i("Paired with " , device.getName());
		                	discoveredArrayAdapter.add(device.getName() + "\nPaired");
	                		break;
	                	case BluetoothDevice.BOND_NONE:
	                		Log.i("Unpaired with " , device.getName());
		                	discoveredArrayAdapter.add(device.getName() + "\nPair with this device");
		                	break;
		                default:
		                	discoveredArrayAdapter.add(device.getName());
	    		    	}
	                    arrayListDiscoveredDevices.add(device);
	                    discoveredArrayAdapter.notifyDataSetChanged();
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
	            case BluetoothAdapter.STATE_TURNING_OFF:
	            	stateBluetooth.setText("Turning Bluetooth off...");
	            	discoveredArrayAdapter.clear();
	            	textScanDevice.setText("");
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
		    else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
		    	BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    	int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
		    	switch (state) {
                	case BluetoothDevice.BOND_BONDED:
                		Log.i("Paired with " , device.getName());
                		Toast.makeText(context, "Paired with " + device.getName(), Toast.LENGTH_SHORT).show();
                		break;
                	case BluetoothDevice.BOND_NONE:
                		Log.i("Unpaired with " , device.getName());
                		
                		break;
                	case BluetoothDevice.BOND_BONDING:
                		Log.i("Pairing with " , device.getName());
                		Toast.makeText(context, "Pairing with " + device.getName(), Toast.LENGTH_SHORT).show();
                }
			}
		}
	};
	
	private void sendMessage(View v) throws IOException{  
		try {
			BluetoothDevice device = pairedBluetoothDevice;
			UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(applicationUUID);
	        socket.connect();
	        String message =  "Hello Amar-PC, gladiator says hello";
	        byte[] sb = message.getBytes(message);
	        outputStream = socket.getOutputStream();
	        outputStream.write(sb);
	        outputStream.flush();
	        Log.i("Message Sending", "Sent");
	     }catch (IOException e) {
	                Log.i("Error", "Exception during write");
	   }
	}
}


	