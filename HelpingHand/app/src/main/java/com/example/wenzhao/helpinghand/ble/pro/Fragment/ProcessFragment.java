package com.example.wenzhao.helpinghand.ble.pro.Fragment;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ti.ble.sensortag.R;
import com.example.wenzhao.helpinghand.ble.pro.ACCInfo.SensorTagGatt;
import com.example.wenzhao.helpinghand.ble.pro.BLEManager.BluetoothLeService;
import com.example.wenzhao.helpinghand.ble.pro.BLEManager.GenericBluetoothProfile;
import com.example.wenzhao.helpinghand.ble.pro.HelpingHand.MainActivity;
import com.example.wenzhao.helpinghand.ble.pro.HelpingHand.ResultActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ProcessFragment extends Fragment {
    // BLE
    private BluetoothLeService mBtLeService = null;
    private ArrayList<BluetoothDevice> mBluetoothDevice = null;
    private ArrayList<BluetoothGatt> mBtGatt = null;
    private List<GenericBluetoothProfile> mProfiles;
    private  double realtimesum1;
    private  double realtimesum2;
    public static TextToSpeech mTts;
    private final static int CHECK_CODE = 1;

    //GUI
    private TextView infoText = null;
    private TextView ratioText1 = null;
    private ImageView imageView1 = null;
    private ImageView imageView2 = null;
    private ImageView imageView3 = null;

    private Button btnFinish;
    public double ratio;
    public static List<Double> M1OverTime;
    public static List<Double> M2OverTime;

    public static float time = 0;
    long startTime;

    public static ProcessFragment newInstance() {
        ProcessFragment fragment = new ProcessFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        // BLE
        mBtLeService = BluetoothLeService.getInstance();
        mBluetoothDevice = BluetoothLeService.getDevice();
        mBtGatt = BluetoothLeService.getBtGatt();
        mProfiles = new ArrayList<GenericBluetoothProfile>();
        M1OverTime = new ArrayList<Double>();
        M2OverTime = new ArrayList<Double>();
        realtimesum1 = 0;
        realtimesum1 = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_process, container, false);
        ratioText1 = (TextView) view.findViewById(R.id.ratio1);
        infoText = (TextView) view.findViewById(R.id.textView18);
        btnFinish = (Button) view.findViewById(R.id.btn_finish);

        imageView1 = (ImageView) view.findViewById(R.id.imageView3);
        imageView2 = (ImageView) view.findViewById(R.id.imageView4);
        imageView3 = (ImageView) view.findViewById(R.id.imageView5);

        imageView1.setImageResource(R.drawable.tabletop1);
        imageView2.setImageResource(R.drawable.tabletop2);
        imageView3.setImageResource(R.drawable.tabletop3);

        infoText.setText("Keep going, " + InputFragment.ChildName +
                ". Use both hands to make these towers.");

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                for (BluetoothGattService s : MainActivity.serviceList1) {
                    if (s.getUuid().toString().compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                        GenericBluetoothProfile mov1 = new GenericBluetoothProfile(getActivity(), mBluetoothDevice.get(0),mBtGatt.get(0), s, mBtLeService);
                        mProfiles.add(mov1);
                    }
                }
                for (BluetoothGattService s : MainActivity.serviceList2) {
                    if (s.getUuid().toString().compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                        GenericBluetoothProfile mov2 = new GenericBluetoothProfile(getActivity(), mBluetoothDevice.get(1),mBtGatt.get(1), s, mBtLeService);
                        mProfiles.add(mov2);
                    }
                }
                for (final GenericBluetoothProfile p : mProfiles) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            p.enableService();
                            Log.i("Device Activity", " enabled");
                        }
                    });
                }


            }
        });
        worker.start();

        checkTts();

        final Intent mResultIntent = new Intent(getActivity(), ResultActivity.class);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long endTime = System.currentTimeMillis() - startTime;
                time = (float) endTime / 1000;
                getActivity().unregisterReceiver(mGattUpdateReceiver1);
                startActivity(mResultIntent);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(ResultActivity.Replace == 1){
            ResultActivity.Replace = 0;
            FragmentManager fm = getActivity().getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            Fragment activityChoiceFragment = ActivityChoiceFragment.newInstance();
            ft.replace(R.id.DevContainer, activityChoiceFragment);
            ft.addToBackStack("Switch to Activity Choice Fragment");
            ft.commit();
        }
        startTime = System.currentTimeMillis();
        M1OverTime.clear();
        M2OverTime.clear();
        realtimesum1 = 0;
        realtimesum2 = 0;
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY1);
        getActivity().registerReceiver(mGattUpdateReceiver1, fi);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBtGatt.size() != 0) {
            for (BluetoothGatt gatt : mBtGatt) {
                gatt.close();
                gatt = null;
            }
        }
        mBtGatt = null;
        mBluetoothDevice =null;
        getActivity().unregisterReceiver(mGattUpdateReceiver1);
        this.mProfiles = null;
    }

    private final BroadcastReceiver mGattUpdateReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            final int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                    BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                //Log.i("Device Activity","  # 1 sensor notified");
                // Notification
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                for (BluetoothGattCharacteristic tempC : MainActivity.charList1) {
                    if ((tempC.getUuid().toString().equals(uuidStr))) {
                        GenericBluetoothProfile p = mProfiles.get(0);
                        if (p.isDataC(tempC)) {
                            p.didUpdateValueForCharacteristic(tempC, 0);
                            double ax1 = GenericBluetoothProfile.accData1.x;
                            double ay1 = GenericBluetoothProfile.accData1.y;
                            double az1 = 1+GenericBluetoothProfile.accData1.z;
                        }
                        break;
                    }
                }
            }

            if (BluetoothLeService.ACTION_DATA_NOTIFY1.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                for (BluetoothGattCharacteristic tempC : MainActivity.charList2) {
                    if ((tempC.getUuid().toString().equals(uuidStr))) {
                        GenericBluetoothProfile p = mProfiles.get(1);
                        if (p.isDataC(tempC)) {
                            p.didUpdateValueForCharacteristic(tempC, 1);
                            double ax2 = GenericBluetoothProfile.accData2.x;
                            double ay2 = GenericBluetoothProfile.accData2.y;
                            double az2 = 1+GenericBluetoothProfile.accData2.z;
                            double ax1 = GenericBluetoothProfile.accData1.x;
                            double ay1 = GenericBluetoothProfile.accData1.y;
                            double az1 = 1+GenericBluetoothProfile.accData1.z;


                            if(Math.sqrt(ax2*ax2 + ay2*ay2 + az2*az2)>0.06){
                                M2OverTime.add(Math.sqrt(ax2 * ax2 + ay2 * ay2 + az2 * az2));
                                realtimesum2 += Math.sqrt(ax2 * ax2 + ay2 * ay2 + az2 * az2);
                            }else{
                                M2OverTime.add(0.0);
                            }

                            if (Math.sqrt(ax1*ax1 + ay1*ay1 + az1*az1)>0.06){
                                M1OverTime.add(Math.sqrt(ax1*ax1 + ay1*ay1 + az1*az1));
                                realtimesum1 +=Math.sqrt(ax1*ax1 + ay1*ay1 + az1*az1);
                            }else{
                                M1OverTime.add(0.0);
                            }
                            if (realtimesum1<realtimesum2) {
                                ratio = 100 *realtimesum1/(realtimesum1+realtimesum2);
                            }else {
                                ratio = 100 *realtimesum2/(realtimesum1+realtimesum2);
                            }
                            ratioText1.setText("Weak arm(" + InputFragment.WeakArm
                                    + ")" + String.format(":%.2f", ratio) + "%");

                            // real-time feedback
                            if ((System.currentTimeMillis()-startTime<15050) && (System.currentTimeMillis()-startTime>15000) && (ratio<20.0)){
                                if (!InputFragment.AbleToRead){
                                    String text = "Please use your "+InputFragment.WeakArm + " more.";
                                    sayTts(text);
                                }else {
                                    String text = "Please use your "+InputFragment.WeakArm + " more.";
                                    Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    };

    //~~~~~~~~~~~~~~~~~~~~~~TextToSpeech~~~~~~~~~~~~~~~~~~~~~~~~~//
    public void checkTts(){
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, CHECK_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CHECK_CODE){
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){

                mTts = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {

                        if(status == TextToSpeech.SUCCESS){

                            int result = mTts.setLanguage(Locale.US);

                            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                                Log.e("error","not supported");
                            }
                        }
                    }
                });
            }else{
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
    private void sayTts(String text){
        mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static TextToSpeech getTts(){
        return mTts;
    }

}