package com.example.wenzhao.helpinghand.ble.pro.HelpingHand;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.example.ti.ble.sensortag.R;
import com.example.wenzhao.helpinghand.ble.pro.BLEManager.BluetoothLeService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

	// Requests to other activities
	private static final int REQ_ENABLE_BT = 0;
	private static final int REQ_DEVICE_ACT = 1;
	// GUI
	private ScanView mScanView;
	private BluetoothLeService mBtLeService = null;
	// BLE management
	private boolean mScanning = false;
	private List<BluetoothDevice> mDeviceList;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBtAdapter = null;
	private List<BluetoothDevice> mBluetoothDeviceList = null;
	private BluetoothLeService mBluetoothLeService = null;
	public static List<BluetoothGattService> serviceList1 = new ArrayList<BluetoothGattService>();
	public static List<BluetoothGattService> serviceList2 = new ArrayList<BluetoothGattService>();
	public static List<BluetoothGattCharacteristic> charList1 = new ArrayList<BluetoothGattCharacteristic>();
	public static List<BluetoothGattCharacteristic> charList2 = new ArrayList<BluetoothGattCharacteristic>();
	private int dnum = 0;
	private int connectedNum = 0;
	private IntentFilter mFilter;

	private boolean mInitialised = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent bindIntent = new Intent(this, BluetoothLeService.class);
		startService(bindIntent);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_view);
		mDeviceList = new ArrayList<BluetoothDevice>();
		mBluetoothDeviceList = new ArrayList<BluetoothDevice>();
		//开启ScanView Fragment
		mScanView = ScanView.newInstance();
		getSupportFragmentManager().beginTransaction()
				.add(R.id.MainContainer, mScanView).commit();
		//初始化广播
		mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED1);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED2);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		mBtAdapter = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.opt_bt:
				Intent settingsIntent = new Intent(
						android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
				startActivity(settingsIntent);
				break;
			case R.id.opt_exit:
				Toast.makeText(this, "Exit...", Toast.LENGTH_SHORT).show();
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	//扫描前从服务中获取蓝牙设备 并注册广播
	void onScanViewReady() {
		if (!mInitialised) {
			mBluetoothLeService = BluetoothLeService.getInstance();
			mBluetoothManager = mBluetoothLeService.getBtManager();
			mBtAdapter = mBluetoothManager.getAdapter();

			registerReceiver(mReceiver, mFilter);
			boolean mBtAdapterEnabled;
			mBtAdapterEnabled = mBtAdapter.isEnabled();
			if (!mBtAdapterEnabled) {
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQ_ENABLE_BT);
			}
			mInitialised = true;
		}
	}
	//按键响应函数 点击后先清空当前list内容 然后开启BLE设备扫描
	public void onBtnScan(View view) {
		onScanViewReady();
		mDeviceList.clear();
		mScanView.notifyDataSetChanged();

		mScanning = mBtAdapter.startLeScan(mLeScanCallback);
	}

	public void onDeviceClick(final int pos) {
		mBluetoothDeviceList.add(mDeviceList.get(pos));
		connectedNum++;
		Log.e("connectedNum",String.valueOf(connectedNum));
		if(connectedNum == 1) mBluetoothLeService.connect(mBluetoothDeviceList.get(0).getAddress());
		if(connectedNum == 2) mBluetoothLeService.connect(mBluetoothDeviceList.get(1).getAddress());
		if (connectedNum == 2){
			if (mScanning) {
				mScanning = false;
				mBtAdapter.stopLeScan(mLeScanCallback);
				connectedNum = 0;
			}
		}
	}
	//处理相应新开启Activity结束后返回的结果
	//1.请求开启DeviceActivity: 断开当前的BLE服务
	//2.请求开启蓝牙： Toast相应的结果（开启成功与否），若没有开启成功，则关闭APP
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQ_DEVICE_ACT:
				mBluetoothLeService.disconnect(mBluetoothDeviceList.get(0).getAddress());
				mBluetoothLeService.disconnect(mBluetoothDeviceList.get(1).getAddress());
				mBluetoothDeviceList.clear();
				break;
			case REQ_ENABLE_BT:
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(this, R.string.bt_on, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.bt_not_on, Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
			default:
				break;
		}
	}
	//当蓝牙适配器和BLE服务状态改变时 接收其所发出的广播 并进行相应处理
	//1.蓝牙适配器改变为关闭状态时：关闭APP
	//2.BLE服务状态改变为连接成功时： 开启DeviceActivity
	//3.BLE服务状态改变为失去连接时： 关闭DeviceActivity，并且关闭BLE服务
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			final String action = intent.getAction();

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				switch (mBtAdapter.getState()) {
					case BluetoothAdapter.STATE_OFF:
						Toast.makeText(context, R.string.app_closing, Toast.LENGTH_LONG)
								.show();
						finish();
						break;
					default:
						break;
				}
			}else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				stopDeviceActivity();
				mBluetoothLeService.close();
			}
			else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED1.equals(action)) {
				int status2 = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
						BluetoothGatt.GATT_SUCCESS);
				if (status2 == BluetoothGatt.GATT_SUCCESS) {

					mBtLeService = BluetoothLeService.getInstance();
					for (BluetoothGattService service : mBtLeService.getSupportedGattServices()) {
						serviceList1.add(service);
					}

					if (serviceList1.size() > 0) {
						for (BluetoothGattService s : serviceList1) {
							List<BluetoothGattCharacteristic> c = s.getCharacteristics();
							charList1.addAll(c);
						}
					}
					dnum++;
					if (dnum == 2) {
						startDeviceActivity();
						dnum = 0;
					}
				}

			}
			else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED2.equals(action)) {
				int status2 = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
						BluetoothGatt.GATT_SUCCESS);
				if (status2 == BluetoothGatt.GATT_SUCCESS) {

					mBtLeService = BluetoothLeService.getInstance();
					for (BluetoothGattService service : mBtLeService.getSupportedGattServices()) {
						serviceList2.add(service);
					}

					if (serviceList2.size() > 0) {
						for (BluetoothGattService s : serviceList2) {
							List<BluetoothGattCharacteristic> c = s.getCharacteristics();
							charList2.addAll(c);
						}
					}
					dnum++;
					if (dnum == 2) {
						startDeviceActivity();
						dnum = 0;
					}
				}
			}
		}
	};
	//蓝牙扫描回调函数 当扫描到新的设备时 通过设备MAC地址判断是否为新扫描到的SensorTag
	//如果是则将新的设备信息加入List并刷新屏幕显示
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		public void onLeScan(final BluetoothDevice device, final int rssi,
							 byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				public void run() {
					String deviceName = device.getName();
					if (deviceName == null)
						return;
					if (deviceName.equals("CC2650 SensorTag")) {
						if (!deviceInfoExists(device.getAddress())) {
							mDeviceList.add(device);
							mScanView.notifyDataSetChanged();
						}
					}
				}
			});
		}
	};

	////////////////////
	//////辅助函数//////
	///////////////////

	//判断当前MAC地址对应的设备是否已经在list中
	private boolean deviceInfoExists(String address) {
		for (int i = 0; i < mDeviceList.size(); i++) {
			if (mDeviceList.get(i).getAddress()
					.equals(address)) {
				return true;
			}
		}
		return false;
	}
	List<BluetoothDevice> getDeviceList() {
		return mDeviceList;
	}
	private void startDeviceActivity() {
		Intent mDeviceIntent = new Intent(this, DeviceActivity.class);
		startActivityForResult(mDeviceIntent, REQ_DEVICE_ACT);

	}
	private void stopDeviceActivity() {
		finishActivity(REQ_DEVICE_ACT);
	}
}
