package edu.purdue.engineering.infraratcontroller;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.bluetooth.BluetoothServerSocket;

public class BluetoothInterface {
	public static enum ReceivingType {IDLE, FUEL, IR_DATA};
	
	public static final int MSG_FUEL = 0;
	public static final int MSG_IR_DATA = 1;
	public static final int MSG_DEVICE_NAME = 2;
	public static final int MSG_CONNECTION_LOST = 3;
	public static final int MSG_CONNECTION_FAILED = 4;
	
	BluetoothAdapter adapter;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String MY_NAME = "InfraratController";
	private int state;
	
	public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    //private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private ConnectThread connectThread;
    
    private final Handler messHandler;
    
    private static final String NO_CONNECTION = "Error: Could not make a bluetooth connection with the car";
    private static final String CONNECTION_FAILED = "Error: Bluetooth connection failed";
    private static final String CONNECTION_LOST = "Error: Bluetooth connection with the car has been lost";
    
    
    public static final byte BATTERY_LEVEL_COMMAND_SRT = (byte)0xBA; // defines the start for the battery level
    public static final byte BATTERY_LEVEL_COMMAND_END = (byte)0x45; // defines the end of the battery level
    public static final byte IR_DATA_COMMAND_SRT = (byte)0x80;
    public static final byte IR_DATA_COMMAND_END = (byte)0x7F;
	
	ReceivingType dataReceiving;
    
    public BluetoothInterface(Handler handler)
    {
    	Initialize();
    	state = STATE_NONE;
    	messHandler = handler;
    }
	
	private void Initialize()
	{
		adapter = BluetoothAdapter.getDefaultAdapter();
		return;
	}
	
	public boolean startDiscovery()
	{
		if(adapter.isDiscovering()) adapter.cancelDiscovery();
		return adapter.startDiscovery();
	}
	
	public boolean CheckForAdapter()
	{
		if(adapter == null)
		{
			return false;
		}
		return true;
	}
	
	public boolean CheckAdapterEnabled()
	{
		if(adapter.isEnabled())
		{
			return true;
		}
		
		return false;
	}
	
	public synchronized void setState(int s)
	{
		state = s;
	}
	
	public synchronized int getState()
	{
		return state;
	}
	
	/*public synchronized void start()
	{
		Log.i("INFRARATCONTROLLER", "BLUETOOTH START");
        if (connectThread != null) 
        {
        	connectThread.cancel();
        	connectThread = null;
        }

        if (connectedThread != null)
        {
        	connectedThread.cancel();
        	connectedThread = null;
        }

        setState(STATE_LISTEN);

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        
	}*/
	
	// used to start the connect thread which will attempt to connect to a bluetooth device
	// input: bluetooth device to connect to
	public synchronized void connect(BluetoothDevice device)
	{
        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        Log.i("INFRARATCONTROLLER", "Connecting to " + device.getName());
        setState(STATE_CONNECTING);
    }
	
	// stops the connection attempt thread and the connected thread
	public synchronized void stop()
	{
        if (connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        /*if (acceptThread != null)
        {
            acceptThread.cancel();
            acceptThread = null;
        }*/
        setState(STATE_NONE);
    }
	
	public synchronized void connected(BluetoothSocket btSocket, BluetoothDevice btDevice)
	{
		if(connectThread != null)
		{
			connectThread.cancel();
			connectThread = null;
		}
		
		if(connectedThread != null)
		{
			connectedThread.cancel();
			connectedThread = null;
		}
		/*if(acceptThread != null)
		{
			acceptThread.cancel();
			acceptThread = null;
		}*/
		
		connectedThread = new ConnectedThread(btSocket);
		connectedThread.start();
		Log.i("INFRARATCONTROLLER", "Connected to bluetooth device: " + btDevice.getName());
		messHandler.obtainMessage(MSG_DEVICE_NAME, btDevice.getName()).sendToTarget();
		
		setState(STATE_CONNECTED);
	}
	
	// used to send data to the connected thread to send data to the bluetooth module
	// takes an input array of bytes
	public void Write(byte[] bytesToWrite)
	{
		ConnectedThread temp;
		synchronized(this)
		{
			if(state != STATE_CONNECTED)
			{
				return;
			}
			temp = connectedThread;
		}
		temp.write(bytesToWrite);
	}
	
	// tells the main activity that the connection attempt failed and stops all connecting or connected threads
	private void connectionFailed() {
		this.stop();
		messHandler.obtainMessage(MSG_CONNECTION_FAILED, CONNECTION_FAILED).sendToTarget();
    }

	// tells the main activity that the connection was lost and stops the connected thread
    private void connectionLost() {
    	this.stop();
        messHandler.obtainMessage(MSG_CONNECTION_LOST, CONNECTION_LOST).sendToTarget();
    }
    
	// thread that connects to the selected bluetooth device
    // this thread blocks until a connection can be made
    // if the connection attempt fails it sends a message back to the
    // activity and this thread will stop
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket btSocket;
		private final BluetoothDevice btDevice;
		
		public ConnectThread(BluetoothDevice device)
		{
			BluetoothSocket temp = null;
			btDevice = device;
			
			try{
				temp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
				Log.i("INFRARATCONTROLLER", "created insecure socket");
			} catch(Exception e)
			{
				Log.e("INFRARATCONTROLLER", "create() failed", e);
			}
			
			btSocket = temp;
		}
		
		public void run()
		{
			Log.i("InfraratController", "BEGIN mConnectThread");
			this.setName("ConnectThread");
			adapter.cancelDiscovery();
			
			try
			{
				Log.i("INFRARATCONTROLLER", "starting connect()");
				btSocket.connect();
				Log.i("INFRARATCONTROLLER", "connected");
			} catch(Exception e)
			{
				Log.e("INFRARATCONTROLLER", "unable to connect() socket", e);
				try
				{
					btSocket.close();
				} catch(Exception e2)
				{
					Log.e("INFRARATCONTROLLER", "unable to close() socket after connect() failed", e2);
				}
				connectionFailed();
				return;
			}
			connected(btSocket, btDevice);
		}
		
		//closes the socket if it is open so the thread can be deleted
		public void cancel()
		{
			try
			{
				btSocket.close();
			} catch(Exception e)
			{
				Log.e("InfraratController", "unable to close() socket", e);
			}
		}
	}
	
	/* This thread is run when a connection is already established
	 * It takes in a bluetooth socket that has been connected with the remote bluetooth device
	 * and listens for data to read
	 * the main activity can also call write data to the socket
	 */
	public class ConnectedThread extends Thread
	{
		private final BluetoothSocket btSocket;
		private final InputStream inputStream;
		private final OutputStream outputStream;
		
		public ConnectedThread(BluetoothSocket socket)
		{
			btSocket = socket;
			InputStream tempIn = null;
			OutputStream tempOut = null;
			
			try
			{
				tempIn = btSocket.getInputStream();
				tempOut = btSocket.getOutputStream();
			} catch(Exception e)
			{
				Log.e("InfraratController", "unable to get input streams");
			}
			
			inputStream = tempIn;
			outputStream = tempOut;
		}
		
		//listens for new data
		public void run()
		{
			byte[] buffer = new byte[1024]; //input buffer from the socket
			byte[] cachedData = new byte[256]; // cached data from the buffer the determine sort the data out
			int cachedPos = 0; // position in the cached data buffer
			int bytes; // number of bytes read from socket
			int buffCounter; // counter for socket buffer
			dataReceiving = ReceivingType.IDLE; // initializes the data it is receiving to idle
			
			while(true)
			{
				try
				{
					bytes = inputStream.read(buffer);
					buffCounter = 0;
					// goes to which state the socket reader is in (what data it is reading)
					// data can be split into multiple packets so the data read may not be all of the
					// needed data (it may need more reads which means that it needs to keep the state that
					// it last read data)
					switch(dataReceiving)
					{
					case IDLE:
						// looks for a command where 
						for(; buffCounter < bytes; buffCounter++)
						{
							if(buffer[buffCounter] == BATTERY_LEVEL_COMMAND_SRT)
							{
								dataReceiving = ReceivingType.FUEL;
								break;
							}
							else if(buffer[buffCounter] == IR_DATA_COMMAND_SRT)
							{
								dataReceiving = ReceivingType.IR_DATA;
								break;
							}
						}
					// sorts out the data if the command was an IR data command
					case IR_DATA:
						for(; buffCounter < bytes; buffCounter++)
						{
							// stops reading from the buffer when an end command is reached
							// sends the data to the main activity
							if(buffer[buffCounter] == IR_DATA_COMMAND_END)
							{
								this.SendDataToActivity(cachedData);
								cachedPos = 0;
								dataReceiving = ReceivingType.IDLE;
								break;
							}
							cachedData[cachedPos++] = buffer[buffCounter];
						}
						break;
					// sorts out the data if the command was a fuel start command
					case FUEL:
						for(; buffCounter < bytes; buffCounter++)
						{
							// stops reading from the buffer if an end command is reached
							// sends the data to the main activity
							if(buffer[buffCounter] == BATTERY_LEVEL_COMMAND_END)
							{
								this.SendDataToActivity(cachedData);
								cachedPos = 0;
								dataReceiving = ReceivingType.IDLE;
								break;
							}
							cachedData[cachedPos] = buffer[buffCounter];
						}
						break;
					}
				} catch(Exception e)
				{
					Log.e("InfraratController", "unable to read() from inputStream", e);
					connectionLost();
				}
			}
		}
		
		// allows the main activity to write to the output stream
		public void write(byte[] buffer)
		{
			try
			{
				outputStream.write(buffer);
			} catch(Exception e)
			{
				Log.e("InfraratController", "unable to write() to outputStream", e);
			}
		}
		
		public void cancel()
		{
			try
			{
				btSocket.close();
			} catch(Exception e)
			{
				Log.e("InfraratController", "unable to close() socket", e);
			}
		}
		
		// sends the data to the main activity through the message handler
		// tells the main activity what type of data it is
		private void SendDataToActivity(byte[] buffer)
		{
			messHandler.obtainMessage((dataReceiving==ReceivingType.FUEL) ? MSG_FUEL : MSG_IR_DATA, buffer).sendToTarget();
		}
	}
}
