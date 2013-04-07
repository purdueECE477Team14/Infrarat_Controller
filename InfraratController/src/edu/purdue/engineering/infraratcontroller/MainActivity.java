package edu.purdue.engineering.infraratcontroller;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends TabActivity implements OnTabChangeListener, OnTouchListener {
	static enum CarMode {flee, follow, manual}; // enumeration for the modes of the car
	static boolean isCarOn = false; // is the car in standby or not
	
	public static final byte CHANGE_MODE_COMMAND_SRT = (byte)0xAA; // defines the start of a command for the bt interface
    public static final byte CHANGE_MODE_COMMAND_END = (byte)0x55; // defines the end of a command for the bt interface
    public static final byte MANUAL_MODE_COMMAND = (byte)0x4D; // defines the manual mode command for the bt interface
    public static final byte FOLLOW_MODE_COMMAND = (byte)0x4F; // defines the follow mode command for the bt interface
    public static final byte FLEE_MODE_COMMAND = (byte)0x46; // defines the flee mode command for the bt interface
	
	static final int NUM_PIXELS = 256; // the total number of pixels to be displayed from the IR sensors
	
	static final String NOBTERROR = "Error: Need bluetooth for app to function"; // the string which tells the user that bluetooth needs to be turned on for the app to work
	static final String NO_BT_ADAPTER_ERROR = "Error: Phone does not have a bluetooth adapter, app will not function without bluetooth"; // string which tells the user that there is no bluetooth adapter
	
	final int REQUEST_ENABLE_BT = 0;
	static CarMode mode; // defines the current state of the car
	
	TabHost tabHost;
	static final String CARTABID = "carTab"; // the id of the car controls tab, contains the ir display and manual controls
	static final String SETTINGSTABID = "settingsTab"; // the id of the settings tab, contains the controls to switch modes
	
	static BluetoothInterface btInterface;
	
	private Pixel pixels[]; // defines the array of pixels that the ir sensor has to be displayed
	static private AnalogControl leftAnalog; // defines the left analog stick control
	static private AnalogControl rightAnalog; // defines the right analog stick control
	
	static private BatteryGauge batteryGauge; // defines the battery gauge for the car
	
	static private int batteryLevel = 100;
	
	private ArrayAdapter bt_device_list;
	
	public List<BluetoothDevice> bluetoothDevicesFound;
	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btInterface = new BluetoothInterface(messHandler);
		
		tabHost = this.getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec(SETTINGSTABID).setIndicator("Settings").setContent(R.id.settingsLayout));
		tabHost.addTab(tabHost.newTabSpec(CARTABID).setIndicator("Car Control").setContent(R.id.carLayout));
		tabHost.setOnTabChangedListener(this);
		
		ListView bt_device_list_view = (ListView) this.findViewById(R.id.bt_list_view);
		bt_device_list = new ArrayAdapter<String>(this, R.layout.list_view_entry);
		bt_device_list_view.setAdapter(bt_device_list);
		bt_device_list_view.setOnItemClickListener(btClickedHandler);
		
		bluetoothDevicesFound = new ArrayList<BluetoothDevice>();
	}
	
	protected void onStart()
	{
		super.onStart();
		
		startBluetoothConnection();
	}
	
	private void createBatteryGauge()
	{
		final int WIDTH = 55;
		final int HEIGHT = 35;
		final int ANALOG_PADDING = 30;
		RelativeLayout carLayout = (RelativeLayout) this.findViewById(R.id.carLayout);
		int viewWidth = tabHost.getWidth();
		int viewHeight = tabHost.getHeight() - this.getTabWidget().getHeight();
		
		batteryGauge = new BatteryGauge(carLayout.getContext(), viewWidth/2-WIDTH/2, viewHeight-ANALOG_PADDING-HEIGHT, WIDTH, HEIGHT, batteryLevel);
		carLayout.addView(batteryGauge);
	}
	
	private void createAnalogSticks()
	{
		final int PADDING = 30;
		final int SIZE = 153;
		RelativeLayout carLayout = (RelativeLayout) this.findViewById(R.id.carLayout);
		int viewWidth = tabHost.getWidth();
		int viewHeight = tabHost.getHeight() - this.getTabWidget().getHeight();
		
		
		rightAnalog = new AnalogControl(carLayout.getContext(), viewWidth-PADDING-SIZE, viewHeight-SIZE-PADDING,SIZE, "right");
		leftAnalog = new AnalogControl(carLayout.getContext(), PADDING,viewHeight-SIZE-PADDING,SIZE,"left");
		carLayout.addView(leftAnalog);
		carLayout.addView(rightAnalog);
		carLayout.setOnTouchListener(this);
	}
	
	private void changePixels()
	{
		int pixelStartX;
		int pixelStartY;
		int pixelSize;
		
		int padding = 30;
		
		pixelSize = (tabHost.getWidth()-padding*2) / 16;
		pixelStartX = padding;
		pixelStartY = padding;
		int count = 0;
		
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 8; j++)
			{
				for(int k = 0; k < 16; k++)
				{
					pixels[count].changePixel(pixelStartX + pixelSize*k, pixelStartY+j*pixelSize, pixelSize);
					count++;
				}
			}
			pixelStartY = pixels[count-1].getBottomShape()+50;
		}
		redrawPixels();
	}
	
	private void createPixels()
	{
		int pixelStartX;
		int pixelStartY;
		int pixelSize;
		
		pixels = new Pixel[NUM_PIXELS];
		
		RelativeLayout carLayout = (RelativeLayout) this.findViewById(R.id.carLayout);
		
		int padding = 30;
		
		pixelSize = (tabHost.getWidth()-padding*2) / 16;
		pixelStartX = padding;
		pixelStartY = padding;
		int count = 0;
		
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 8; j++)
			{
				for(int k = 0; k < 16; k++)
				{
					pixels[count] = new Pixel(carLayout.getContext(), pixelStartX + pixelSize*k, pixelStartY+j*pixelSize, pixelSize);
					carLayout.addView(pixels[count]);
					count++;
				}
			}
			pixelStartY = pixels[count-1].getBottomShape()+50;
		}
		
//		for(int i = 0; i < 8; i++)
//		{
//			for(int j = 0; j < 32; j++)
//			{
//				pixels[count] = new Pixel(carLayout.getContext(), pixelStartX + pixelSize*j, pixelStartY+i*pixelSize, pixelSize);
//				carLayout.addView(pixels[count]);
//				count++;
//			}
//		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus)
		{
			if(tabHost == null) return;
			String tabId = tabHost.getCurrentTabTag();
			if(tabId == null) return;
			if(tabId == CARTABID)
			{
				// changes the position and size of the pixels on a screen change if they are already created
				// this is because it will so the device down if it keeps recreating a bunch of new pixels
				if(pixels != null)
				{
					changePixels();
				} else
				{
					createPixels();
				}
				createAnalogSticks();
				createBatteryGauge();
			}
		}
	}
	
	
	private void EnableBluetooth()
	{
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == REQUEST_ENABLE_BT)
		{
			if(resultCode == Activity.RESULT_CANCELED)
			{
				ShowError("Error: Need bluetooth to control car");
			}
		}
	}
	
	// starts the bluetooth interface and enables discovery
	private void startBluetoothConnection()
	{
		if(!btInterface.CheckForAdapter())
		{
			ShowError(NO_BT_ADAPTER_ERROR);
		}
		else
		{
			if(!btInterface.CheckAdapterEnabled())
			{
				EnableBluetooth();
			}
			this.bluetoothDevicesFound = new ArrayList<BluetoothDevice>();
			this.bt_device_list.clear();
			
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			try {
				registerReceiver(mReceiver, filter);
			} catch(Exception e)
			{
				Log.e("INFRARATCONTROLLER", "error in registering bluetooth discovery receiver", e);
			}
			btInterface.startDiscovery();
		}
	}
	
	//copied from Android Reference guide
	//http://developer.android.com/guide/topics/connectivity/bluetooth.html
	//adds a bluetooth device to the discovered devices list
	//lets the user select which device to connect to
	// gets called when discovery finds a new device that has its name start with Infrarat
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            // Add the name and address to an array adapter to show in a ListView
	            Log.i("INFRARATCONTROLLER", "Device found: " + device.getName());
	            if(device.getName().startsWith("Infrarat"))
	            {
	            	bluetoothDevicesFound.add(device);
	            	bt_device_list.add(device.getName());
	            }
	        }
	    }
	};
	
	// trys to connect to the selected bluetooth device
	// uses the id of the selected list entry to connect to the device in the device list
	private OnItemClickListener btClickedHandler = new OnItemClickListener() {
	    public void onItemClick(AdapterView parent, View v, int position, long id) {
	    	Log.i("INFRARATCONTROLLER", "Item Clicked");
	    	if(btInterface.getState() != btInterface.STATE_CONNECTING && btInterface.getState() != btInterface.STATE_CONNECTED)
	    	{
	    		btInterface.connect(bluetoothDevicesFound.get(position));
	    	}
	    }
	};

	// used to show an error message
	public void ShowError(String msg)
	{
		Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
	}
	
	public void onRadioButtonClicked(View view)
	{
		boolean checked = ((RadioButton)view).isChecked();
		
		switch(view.getId())
		{
		case R.id.fleeRB :
			if(checked) {
				ChangeToFleeMode();
			}
			break;
		case R.id.followRB :
			if(checked) {
				ChangeToFollowMode();
			}
			break;
		case R.id.manualRB :
			if(checked) {
				ChangeToManualMode();
			}
			break;
		}
	}
	
	// tells the car that it needs to change to flee mode
	private void ChangeToFleeMode()
	{
		mode = CarMode.flee;
		byte[] writeBytes = new byte[3];
		writeBytes[0]=CHANGE_MODE_COMMAND_SRT;
		writeBytes[1]=FLEE_MODE_COMMAND;
		writeBytes[2]=CHANGE_MODE_COMMAND_END;
		btInterface.Write(writeBytes);
	}
	
	// tells the car that it needs to change to follow mode
	private void ChangeToFollowMode()
	{
		mode = CarMode.follow;
		byte[] writeBytes = new byte[3];
		writeBytes[0]=CHANGE_MODE_COMMAND_SRT;
		writeBytes[1]=FOLLOW_MODE_COMMAND;
		writeBytes[2]=CHANGE_MODE_COMMAND_END;
		btInterface.Write(writeBytes);
	}
	
	// tells the car that it needs to change to manual mode
	private void ChangeToManualMode()
	{
		mode = CarMode.manual;
		byte[] writeBytes = new byte[3];
		writeBytes[0]=CHANGE_MODE_COMMAND_SRT;
		writeBytes[1]=MANUAL_MODE_COMMAND;
		writeBytes[2]=CHANGE_MODE_COMMAND_END;
		btInterface.Write(writeBytes);
	}

	// used to invalidate all of the pixel boxes for the ir display
	// needs to be run on the main activity thread
	private void redrawPixels()
	{
			if(pixels == null) return;
			for(int i = 0; i < pixels.length; i++)
			{
				pixels[i].invalidate();
			}
	}

	// creates the car controls and the ir pixel array when the tab is switched to the car control tab
	@Override
	public void onTabChanged(String tabId) {
		if(tabId == CARTABID)
		{
			if(pixels == null)
			{
				createPixels();
			}
			
			if(leftAnalog == null || rightAnalog == null)
			{
				createAnalogSticks();
			}
			
			if(batteryGauge == null)
			{
				createBatteryGauge();
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean returnVal1 = false;
		boolean returnVal2 = false;
		
		
		if(leftAnalog != null)
		{
			returnVal1 = leftAnalog.onTouch(event);
		}
		if(rightAnalog != null)
		{
			returnVal2 = rightAnalog.onTouch(event);
		}
		
		return returnVal1 || returnVal2;
	}
	
	// handles the messages sent to it from the bluetooth interface
	private final Handler messHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
			// updates the battery meter when the bluetooth interface sends data
			case BluetoothInterface.MSG_FUEL :
				byte[] fuelBuff = (byte[]) msg.obj;
				batteryLevel = (int)fuelBuff[0];
				batteryGauge.updateBattery(batteryLevel);
				batteryGauge.invalidate();
				break;
			// gets the ir data from the bluetooth interface and updates the ir display
			case BluetoothInterface.MSG_IR_DATA :
				byte[] irBuff = (byte[]) msg.obj;
				for(int i = 0; i < irBuff.length; i++)
				{
					pixels[i].setGradientFromTemperature((int)irBuff[i]);
				}
				redrawPixels();
				break;
			// displays the name of the device when a connection has been made
			case BluetoothInterface.MSG_DEVICE_NAME :
				Toast.makeText(getApplicationContext(), "Connected to " + msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			// the connection failed or was lost
			// need to re-enable discovery and allow the user to select a new device to connect to
			case BluetoothInterface.MSG_CONNECTION_FAILED :
			case BluetoothInterface.MSG_CONNECTION_LOST :
				startBluetoothConnection();
				Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
}
