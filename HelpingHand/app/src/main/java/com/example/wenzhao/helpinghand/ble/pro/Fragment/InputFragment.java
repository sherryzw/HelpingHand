package com.example.wenzhao.helpinghand.ble.pro.Fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.example.ti.ble.sensortag.R;
import com.example.wenzhao.helpinghand.ble.pro.Database.ChildInfo;

public class InputFragment extends Fragment {
    public static ChildInfo curChild;

    public static String ChildName;
    public static String WeakArm;
    public static boolean AbleToRead;

    Button btnNextInput;
    EditText nickNameText;
    CheckBox checkBoxLeft;
    CheckBox checkBoxRight;
    CheckBox checkBoxYes;
    CheckBox checkBoxNo;

    public static InputFragment newInstance(){
        InputFragment fragment = new InputFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        curChild = new ChildInfo();

        View view = inflater.inflate(R.layout.fragment_input, container, false);
        btnNextInput = (Button)view.findViewById(R.id.btn_next_input);
        nickNameText = (EditText)view.findViewById(R.id.editText);
        checkBoxLeft = (CheckBox)view.findViewById(R.id.checkBoxLeft);
        checkBoxRight = (CheckBox)view.findViewById(R.id.checkBoxRight);
        checkBoxYes = (CheckBox)view.findViewById(R.id.checkBoxYes);
        checkBoxNo = (CheckBox)view.findViewById(R.id.checkBoxNo);

        checkBoxLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkBoxRight.setChecked(false);
                    WeakArm = "Left";
                }else{
                    WeakArm = "";
                }
            }
        });

        checkBoxRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkBoxLeft.setChecked(false);
                    WeakArm = "Right";
                }else{
                    WeakArm = "";
                }
            }
        });

        checkBoxYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkBoxNo.setChecked(false);
                    AbleToRead = true;
                } else {
                    AbleToRead = false;
                }
            }
        });

        checkBoxNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkBoxYes.setChecked(false);
                    AbleToRead =false;
                } else {
                    AbleToRead = false;
                }
            }
        });

        btnNextInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempName = nickNameText.getText().toString();
                ChildName = tempName;
                getActivity().finish();
            }
        });

        return view;
    }



}
