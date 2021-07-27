package com.jxhau.iguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ContactActivity extends AppCompatActivity {
    Button SaveButtonID, EditButtonID;
    TextView textViewContactID, textViewNumID;
    EditText ContactID, PhoneNumID;

    // creating constant keys for shared preferences.
    public static final String SHARED_PREFS = "shared_prefs";
    // key for storing email.
    public static final String CONTACT_NAME = "contact_name";
    // key for storing password.
    public static final String PHONE_NUMBER = "phone_number";
    // variable for shared preferences.
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        SaveButtonID = findViewById(R.id.SaveButtonID);
        EditButtonID = findViewById(R.id.EditButtonID);
        textViewContactID = findViewById(R.id.textViewContactID);
        textViewNumID = findViewById(R.id.textViewNumID);
        ContactID = findViewById(R.id.ContactID);
        PhoneNumID = findViewById(R.id.PhoneNumID);

        // initializing our shared preferences.
        sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        // getting data from shared prefs and storing it in our string variable.
        String contactName = sharedpreferences.getString(CONTACT_NAME, null);
        String phoneNumber = sharedpreferences.getString(PHONE_NUMBER, null);

        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionBar.setHomeButtonEnabled(true);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_baseline_arrow_back_24);
        upArrow.setColorFilter(Color.parseColor("#CE0E2D"), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");

        // textView show the name and number
        if (contactName == null && phoneNumber == null){
            ContactID.setVisibility(View.VISIBLE);
            PhoneNumID.setVisibility(View.VISIBLE);
            textViewContactID.setVisibility(View.GONE);
            textViewNumID.setVisibility(View.GONE);
            SaveButtonID.setEnabled(true);
            EditButtonID.setEnabled(false);
            EditButtonID.setBackgroundColor(Color.parseColor("#35424a"));
        }else {
            ContactID.setVisibility(View.INVISIBLE);
            PhoneNumID.setVisibility(View.INVISIBLE);
            textViewContactID.setVisibility(View.VISIBLE);
            textViewNumID.setVisibility(View.VISIBLE);
            textViewContactID.setText(contactName);
            textViewNumID.setText(phoneNumber);
            SaveButtonID.setEnabled(false);
            EditButtonID.setEnabled(true);
            EditButtonID.setBackgroundColor(Color.parseColor("#CE0E2D"));  //red
        }

        // After Save Button, TextView get editText. EditText invisible and TextView visible.
        SaveButtonID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewContactID.setText(ContactID.getText().toString());
                textViewNumID.setText(PhoneNumID.getText().toString());
                ContactID.setVisibility(View.INVISIBLE);
                PhoneNumID.setVisibility(View.INVISIBLE);
                textViewContactID.setVisibility(View.VISIBLE);
                textViewNumID.setVisibility(View.VISIBLE);
                // to check if the user fields are empty or not.
                if (TextUtils.isEmpty(textViewContactID.getText().toString()) && TextUtils.isEmpty(textViewNumID.getText().toString())) {
                    // this method will call when email and password fields are empty.
                    Toast.makeText(ContactActivity.this, "Please enter contact name and contact number.", Toast.LENGTH_SHORT).show();
                    ContactID.setVisibility(View.VISIBLE);
                    PhoneNumID.setVisibility(View.VISIBLE);
                    textViewContactID.setVisibility(View.GONE);
                    textViewNumID.setVisibility(View.GONE);
                } else {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    // put values for contactname and phonenumber in shared preferences.
                    editor.putString(CONTACT_NAME, ContactID.getText().toString());
                    editor.putString(PHONE_NUMBER, PhoneNumID.getText().toString());
                    // to save our data with key and value.
                    editor.apply();
                    SaveButtonID.setEnabled(false);
                    EditButtonID.setEnabled(true);
                }
                EditButtonID.setBackgroundColor(Color.parseColor("#CE0E2D"));  //red
            }
        });
        // Edit Button, contents of TextView is deleted. EditText visible and TextView invisible.
        EditButtonID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewContactID.setVisibility(View.GONE);
                textViewNumID.setVisibility(View.GONE);
                ContactID.setVisibility(View.VISIBLE);
                PhoneNumID.setVisibility(View.VISIBLE);
                textViewContactID.setText("");
                textViewNumID.setText("");
                SharedPreferences.Editor editor = sharedpreferences.edit();
                // clear the data in shared prefs.
                editor.clear();
                // apply empty data to shared prefs.
                editor.apply();
                SaveButtonID.setEnabled(true);
                EditButtonID.setEnabled(false);
                EditButtonID.setBackgroundColor(Color.parseColor("#35424a"));
            }
        });
    }

    public boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this,permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
}
