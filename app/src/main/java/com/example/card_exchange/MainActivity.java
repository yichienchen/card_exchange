package com.example.card_exchange;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "cardexchange";
    private EditText edit_Name,edit_Company,edit_Phone;
    private Button btn_UserInfo;

    public static String Input_Name,Input_Phone,Input_Company;
    //public static long Input_Company;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edit_Name = (EditText)findViewById(R.id.edit_Name);
        edit_Company = (EditText)findViewById(R.id.edit_Company);
        edit_Phone = (EditText)findViewById(R.id.edit_Phone);
        btn_UserInfo = (Button)findViewById(R.id.btn_UserInfo);

        SharedPreferences setting_name =
                getSharedPreferences("name", MODE_PRIVATE);
        edit_Name.setText(setting_name.getString("pre_name", ""));

        SharedPreferences setting_phone =
                getSharedPreferences("phone", MODE_PRIVATE);
        edit_Phone.setText(setting_phone.getString("pre_phone", ""));

        SharedPreferences setting_company =
                getSharedPreferences("company", MODE_PRIVATE);
        edit_Company.setText(setting_company.getString("pre_company", ""));


        btn_UserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Input_Company = 0;
                Input_Name = edit_Name.getText().toString();
                Input_Phone = edit_Phone.getText().toString();
                Input_Company = edit_Company.getText().toString();
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,BLE.class);
                startActivity(intent);
                MainActivity.this.finish();

                SharedPreferences user_name = getSharedPreferences("name",MODE_PRIVATE);
                user_name.edit()
                        .putString("pre_name",Input_Name)
                        .commit();

                SharedPreferences user_company = getSharedPreferences("company",MODE_PRIVATE);
                user_company.edit()
                        .putString("pre_company",Input_Company)
                        .commit();

                SharedPreferences user_phone = getSharedPreferences("phone",MODE_PRIVATE);
                user_phone.edit()
                        .putString("pre_phone",Input_Phone)
                        .commit();

            }
        });
        Log.d("TAG","Input_Name1: "+Input_Name);
        Log.d("TAG","Input_Phone1: "+Input_Phone);
        Log.d("TAG","Input_Company1: "+Input_Company);

    }
}
