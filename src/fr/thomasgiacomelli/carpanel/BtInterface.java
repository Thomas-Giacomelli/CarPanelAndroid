package fr.thomasgiacomelli.carpanel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BtInterface {

	private BluetoothDevice device = null;
	private BluetoothSocket socket = null;
	private BluetoothAdapter blueAdapter = null;
	private InputStream receiveStream = null;
	private OutputStream sendStream = null;
	
	private ReceiverThread receiverThread;

	Handler handler;

	public BtInterface(Handler hstatus, Handler h) {
		
		blueAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (!blueAdapter.isEnabled()) 
		{
		    blueAdapter.enable();
		}
		
		Set<BluetoothDevice> setpairedDevices = blueAdapter.getBondedDevices();
		
		BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);
		
		for(int i=0;i<pairedDevices.length;i++) {
			if(pairedDevices[i].getName().contains("linvor")) {
				Log.d("Find device", "Find : " + pairedDevices[i].getName());
				device = pairedDevices[i];
				try {
					socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					receiveStream = socket.getInputStream();
					sendStream = socket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}

		handler = hstatus;
		
		receiverThread = new ReceiverThread(h);
	}
	
	public boolean isConnected()
	{
		return socket.isConnected();
	}
	
	public void sendData(String data) {
		sendData(data, false);
	}
	
	public void sendData(String data, boolean deleteScheduledData) {
		try {
			sendStream.write(data.getBytes());
	        sendStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void connect() {
		new Thread() {
			@Override public void run() {
				try {
					socket.connect();
					
					Message msg = handler.obtainMessage();
					msg.arg1 = 1;
	                handler.sendMessage(msg);
	                
					receiverThread.start();
					
				} catch (IOException e) {
					Log.v("N", "Connection Failed : "+e.getMessage());
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void close() {
		try {
			socket.close();
			blueAdapter.disable();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BluetoothDevice getDevice() {
		return device;
	}
	
	private class ReceiverThread extends Thread {
		Handler handler;
		
		ReceiverThread(Handler h) {
			handler = h;
		}
		
		@Override public void run() {
			while(true) {
				try {
					if(receiveStream.available() > 0) {

						byte buffer[] = new byte[100];
						int k = receiveStream.read(buffer, 0, 100);

						if(k > 0) {
							byte rawdata[] = new byte[k];
							for(int i=0;i<k;i++)
								rawdata[i] = buffer[i];
							
							String data = new String(rawdata);

							Message msg = handler.obtainMessage();
							Bundle b = new Bundle();
							b.putString("receivedData", data);
			                msg.setData(b);
			                handler.sendMessage(msg);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
