package com.example.lgpc.waterqualiltydata;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.opencensus.tags.Tag;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, TextView.OnEditorActionListener, View.OnFocusChangeListener, TextWatcher {
    int positionY =0;

    private ScrollView svScroll;
    private EditText etSiteCode, etDepth, etTemp, etPh, etOrp, etConductivity, etTurbidity, etDo, etTds, etComments;
    private TextView tvDate, tvTime;
    private RadioGroup rgSample;
    private String sSample =null;
    private Button btPhoto,btSave;
    private Spinner spinnerFlow;
    private InputMethodManager imm;
    private Context mContext;
    private GoogleMap mMap;
    private Marker currentMarker = null;
    List<Address> addresses;
    private Address address;

    private FirebaseFirestore db;
    private StorageReference mStorageRef;
    private Uri uriPhotos = null;
    private ProgressDialog mProgress;
    private int imageCount=0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable workRunnable;

    private static final int MAX_IMAGE = 5;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500;

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int MY_CAMERA_PERMISSION_CODE =100;

    boolean needRequest = false;

    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    Location mCurrentLocation;
    LatLng currentPosition;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;


    private View mLayout;

    /*public MainActivity() {
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //firebase firestore
        db = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mProgress = new ProgressDialog(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLayout = findViewById(R.id.linear1);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Date
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy/MM/dd");
        String formatDate = sdfDate.format(date);

        //Time
        Date time = new Date(now);
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        String formatTime = sdfTime.format(time);

        imm = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);

        tvDate = findViewById(R.id.textDate);
        tvTime = findViewById(R.id.textTime);

        svScroll = findViewById(R.id.scroll1);
        svScroll.post(new Runnable() {
            @Override
            public void run() {
                svScroll.scrollTo(0,0);
            }
        });

        etSiteCode = findViewById(R.id.editSiteCode);
        etDepth = findViewById(R.id.editDepth);
        etTemp = findViewById(R.id.editTemp);
        etPh = findViewById(R.id.editPh);
        etOrp = findViewById(R.id.editOrp);
        etTurbidity = findViewById(R.id.editTurbidity);
        etConductivity = findViewById(R.id.editConductivity);
        etDo = findViewById(R.id.editDo);
        etTds = findViewById(R.id.editTds);
        etComments = findViewById(R.id.editComments);

        rgSample = findViewById(R.id.radioGroup);

        btSave = findViewById(R.id.buttonSave);
        btPhoto = findViewById(R.id.buttonPhoto);

        spinnerFlow = findViewById(R.id.spinnerFlow);

        tvDate.setText(formatDate);
        tvTime.setText(formatTime);
        etSiteCode.setOnClickListener(this);
        etTemp.setOnClickListener(this);
        etDepth.setOnClickListener(this);
        etPh.setOnClickListener(this);
        etOrp.setOnClickListener(this);
        etTurbidity.setOnClickListener(this);
        etConductivity.setOnClickListener(this);
        etDo.setOnClickListener(this);
        etTds.setOnClickListener(this);
        etComments.setOnClickListener(this);
        btSave.setOnClickListener(this);
        btPhoto.setOnClickListener(this);

        etSiteCode.setOnFocusChangeListener(this);
        etDepth.setOnFocusChangeListener(this);
        etTemp.setOnFocusChangeListener(this);
        etPh.setOnFocusChangeListener(this);
        etOrp.setOnFocusChangeListener(this);
        etTurbidity.setOnFocusChangeListener(this);
        etConductivity.setOnFocusChangeListener(this);
        etDo.setOnFocusChangeListener(this);
        etTds.setOnFocusChangeListener(this);
        etComments.setOnFocusChangeListener(this);

        etDepth.addTextChangedListener(this);
        etTemp.addTextChangedListener(this);
        etPh.addTextChangedListener(this);
        etOrp.addTextChangedListener(this);
        etTurbidity.addTextChangedListener(this);
        etConductivity.addTextChangedListener(this);
        etDo.addTextChangedListener(this);
        etTds.addTextChangedListener(this);

        /*etSiteCode.setOnEditorActionListener(this);
        etDepth.setOnEditorActionListener(this);
        etTemp.setOnEditorActionListener(this);
        etPh.setOnEditorActionListener(this);
        etOrp.setOnEditorActionListener(this);
        etTurbidity.setOnEditorActionListener(this);
        etConductivity.setOnEditorActionListener(this);
        etDo.setOnEditorActionListener(this);
        etTds.setOnEditorActionListener(this);
        etComments.setOnEditorActionListener(this);
*/
        rgSample.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rbYes) {
                    sSample = "Yes";
                } else if(checkedId == R.id.rbNo) {
                    sSample = "No";
                }
            }
        });
/*
        etComments.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId)
                {
                    case EditorInfo.IME_ACTION_DONE:
                        imm.hideSoftInputFromWindow(etComments.getWindowToken(),0);
                        break;
                }
                return false;
            }
        });
*/
    }

    //Press next and done in keyboard
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch (actionId)
        {
            case EditorInfo.IME_ACTION_NEXT:
            case EditorInfo.IME_ACTION_DONE:
                v.setCursorVisible(false);
                break;
        }
        return false;
    }

    //Control the cursor
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus)
        {
            switch (v.getId())
            {
                case R.id.editSiteCode:
                    etSiteCode.setCursorVisible(true);
                    break;
                case R.id.editDepth:
                    etDepth.setCursorVisible(true);
                    break;
                case R.id.editTemp:
                    etTemp.setCursorVisible(true);
                    break;
                case R.id.editPh:
                    etPh.setCursorVisible(true);
                    break;
                case R.id.editOrp:
                    etOrp.setCursorVisible(true);
                    break;
                case R.id.editTurbidity:
                    etTurbidity.setCursorVisible(true);
                    break;
                case R.id.editConductivity:
                    etConductivity.setCursorVisible(true);
                    break;
                case R.id.editDo:
                    etDo.setCursorVisible(true);
                    break;
                case R.id.editTds:
                    etTds.setCursorVisible(true);
                    break;
                case R.id.editComments:
                    etComments.setCursorVisible(true);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        hideCursor();
        /*if (etSiteCode.getText().toString().isEmpty()) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle("Error");
            ab.setMessage("Please fill the site code");
            ab.setCancelable(false);
            ab.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    etSiteCode.requestFocus();
                }
            });
            AlertDialog dialog = ab.create();
            dialog.show();
        } else {*/
            switch (v.getId()) {
                case R.id.editSiteCode:
                    etSiteCode.requestFocus();
                    imm.showSoftInput(etSiteCode, 0);   //keyboard
                    break;

                case R.id.editDepth:
                    etDepth.requestFocus();
                    imm.showSoftInput(etDepth, 0);
                    break;

                case R.id.editTemp:
                    etTemp.requestFocus();
                    imm.showSoftInput(etTemp, 0);
                    break;

                case R.id.editPh:
                    etPh.requestFocus();
                    imm.showSoftInput(etPh, 0);
                    break;

                case R.id.editOrp:
                    etOrp.requestFocus();
                    imm.showSoftInput(etOrp, 0);
                    break;

                case R.id.editTurbidity:
                    etTurbidity.requestFocus();
                    imm.showSoftInput(etTurbidity, 0);
                    break;

                case R.id.editConductivity:
                    etConductivity.requestFocus();
                    imm.showSoftInput(etConductivity, 0);
                    break;

                case R.id.editDo:
                    etDo.requestFocus();
                    imm.showSoftInput(etDo, 0);
                    break;

                case R.id.editTds:
                    etTds.requestFocus();
                    imm.showSoftInput(etTds, 0);
                    break;

                case R.id.editComments:
                    etComments.requestFocus();
                    imm.showSoftInput(etComments, 0);
                    break;

                case R.id.buttonPhoto:
                    //Camera permission
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                    }
                    else
                    {
                        if (etSiteCode.getText().toString().isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Please, enter a site code", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (photoIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(photoIntent, CAMERA_REQUEST_CODE);
                            }
                        }
                    }
                    break;

                case R.id.buttonSave:
                    Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
                    //Save the data in database
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("A. Date", tvDate.getText().toString());
                    dataMap.put("B. Time", tvTime.getText().toString());
                    dataMap.put("C. Location",address.getAddressLine(0).toString());
                    dataMap.put("D. Site Code", etSiteCode.getText().toString());
                    dataMap.put("E. Depth", etDepth.getText().toString());
                    dataMap.put("F. Temperature", etTemp.getText().toString());
                    dataMap.put("G. Ph", etPh.getText().toString());
                    dataMap.put("H. ORP", etOrp.getText().toString());
                    dataMap.put("I. Turbidity", etTurbidity.getText().toString());
                    dataMap.put("J. Conductivity", etConductivity.getText().toString());
                    dataMap.put("K. Do (mg/L)", etDo.getText().toString());
                    dataMap.put("L. TDS", etTds.getText().toString());
                    dataMap.put("M. Flow", spinnerFlow.getSelectedItem().toString());
                    dataMap.put("N. Sample taken?",sSample);
                    dataMap.put("O. Comments", etComments.getText().toString());

                    db.collection("Data")
                            .add(dataMap)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("TAG", "error adding document", e);
                                }
                            });
                    //Reload the application
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;
            }
        //}

    }


    private void hideCursor() {
        etSiteCode.setCursorVisible(false);
        etDepth.setCursorVisible(false);
        etTemp.setCursorVisible(false);
        etPh.setCursorVisible(false);
        etOrp.setCursorVisible(false);
        etConductivity.setCursorVisible(false);
        etTurbidity.setCursorVisible(false);
        etDo.setCursorVisible(false);
        etTds.setCursorVisible(false);
        etComments.setCursorVisible(false);
        //imm.hideSoftInputFromWindow(etComments.getWindowToken(), 0);
    }

    public void warningAlert(final EditText edit)
    {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Warning");
        ab.setMessage("Are you sure about this data?");
        ab.setCancelable(false);
        ab.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                edit.requestFocus();
            }
        });
        ab.setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                edit.setText(null);
            }
        });
        AlertDialog dialog = ab.create();
        dialog.show();
    }

    //In order to make delay
    public final void rulesForpH(String str) {
        try {
            if(Double.parseDouble(str)>10 && Double.parseDouble(str)<14) {
                warningAlert(etPh);
            }
            else if(Double.parseDouble(str)>0 && Double.parseDouble(str)<3) {
                warningAlert(etPh);
            }
            else if(Double.parseDouble(str)>14 || Double.parseDouble(str)<0) {
                etPh.setText("");
                etPh.requestFocus();
                Toast.makeText(getApplicationContext(), "Ph is invalid", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            etPh.setText("");
            etPh.requestFocus();
            Toast.makeText(getApplicationContext(), "Ph is invalid", Toast.LENGTH_LONG).show();
        }

    }

    public final void rulesForOrp (String str) {
        try {
            if(Double.parseDouble(str)>-1000 && Double.parseDouble(str)<-500) {
                warningAlert(etOrp);
            }
            else if(Double.parseDouble(str)>500 && Double.parseDouble(str)<1000) {
                warningAlert(etOrp);
            }
            else if(Double.parseDouble(str)>1000 || Double.parseDouble(str)<-1000) {
                etOrp.setText("");
                etOrp.requestFocus();
                Toast.makeText(getApplicationContext(), "ORP is invalid", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            etOrp.setText("");
            etOrp.requestFocus();
            Toast.makeText(getApplicationContext(), "ORP is invalid", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            etOrp.setText("");
            etOrp.requestFocus();
            Toast.makeText(getApplicationContext(), "ORP is invalid", Toast.LENGTH_LONG).show();
        }

    }

    public final void rulesForConductivity(String str) {
        try {
            if(Double.parseDouble(str)>1 && Double.parseDouble(str)<400) {
                warningAlert(etConductivity);
            }
            else if(Double.parseDouble(str)>400 || Double.parseDouble(str)<0) {
                etConductivity.setText("");
                etConductivity.requestFocus();
                Toast.makeText(getApplicationContext(), "Conductivity is invalid", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            etConductivity.setText("");
            etConductivity.requestFocus();
            Toast.makeText(getApplicationContext(), "Conductivity is invalid", Toast.LENGTH_LONG).show();
        }

    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    //Control the text field based on requirements
    @Override
    public void afterTextChanged(final Editable s) {
        if(etDepth.getText().hashCode() == s.hashCode())
        {
            if(s.toString().length()>0)
            {
                try {
                    if(Double.parseDouble(s.toString())>15 && Double.parseDouble(s.toString())<=50) {
                        warningAlert(etDepth);
                    }
                    else if(Double.parseDouble(s.toString())>50 || Double.parseDouble(s.toString())<0) {
                        etDepth.setText("");
                        etDepth.requestFocus();
                        Toast.makeText(getApplicationContext(), "Depth is invalid", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    etDepth.setText("");
                    etDepth.requestFocus();
                    Toast.makeText(getApplicationContext(), "Depth cannot be negative value", Toast.LENGTH_LONG).show();
                }

            }
        }

        else if(etTemp.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    if(Double.parseDouble(s.toString())>30 && Double.parseDouble(s.toString())<=50) {
                        warningAlert(etTemp);
                    }
                    else if(Double.parseDouble(s.toString())>50 || Double.parseDouble(s.toString())<0) {
                        etTemp.setText("");
                        etTemp.requestFocus();
                        Toast.makeText(getApplicationContext(), "Temperature is invalid", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    etTemp.setText("");
                    etTemp.requestFocus();
                    Toast.makeText(getApplicationContext(), "Temperature cannot be negative value", Toast.LENGTH_LONG).show();
                }

            }
        }

        else if(etPh.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    handler.removeCallbacks(workRunnable);
                    workRunnable = new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.rulesForpH(s.toString());
                        }
                    };
                    handler.postDelayed(workRunnable,1000);
                } catch (NumberFormatException e) {
                    etPh.setText("");
                    etPh.requestFocus();
                    Toast.makeText(getApplicationContext(), "pH cannot be negative value", Toast.LENGTH_LONG).show();
                }

            }
        }

        else if(etOrp.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    handler.removeCallbacks(workRunnable);
                    workRunnable = new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.rulesForOrp(s.toString());
                        }
                    };
                    handler.postDelayed(workRunnable,1000);
                } catch (NumberFormatException e) {
                    etOrp.setText("");
                    etOrp.requestFocus();
                    Toast.makeText(getApplicationContext(), "ORP is invalid", Toast.LENGTH_LONG).show();
                }

            }
        }

        else if(etTurbidity.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    if(Double.parseDouble(s.toString())>200 && Double.parseDouble(s.toString())<400) {
                        warningAlert(etTurbidity);
                    }
                    else if(Double.parseDouble(s.toString())>400 || Double.parseDouble(s.toString())<0) {
                        etTurbidity.setText("");
                        etTurbidity.requestFocus();
                        Toast.makeText(getApplicationContext(), "Turbidity is invalid", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    etTurbidity.setText("");
                    etTurbidity.requestFocus();
                    Toast.makeText(getApplicationContext(), "Turbidity cannot be negative value", Toast.LENGTH_LONG).show();
                }
            }
        }

        else if(etConductivity.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    handler.removeCallbacks(workRunnable);
                    workRunnable = new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.rulesForConductivity(s.toString());
                        }
                    };
                    handler.postDelayed(workRunnable,1000);
                } catch (NumberFormatException e) {
                    etConductivity.setText("");
                    etConductivity.requestFocus();
                    Toast.makeText(getApplicationContext(), "EC cannot be negative value", Toast.LENGTH_LONG).show();
                }
            }
        }

        else if(etDo.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    if(Double.parseDouble(s.toString())>0 && Double.parseDouble(s.toString())<1) {
                        warningAlert(etDo);
                    }
                    else if(Double.parseDouble(s.toString())>11 && Double.parseDouble(s.toString())<15) {
                        warningAlert(etDo);
                    }
                    else if(Double.parseDouble(s.toString())>15 || Double.parseDouble(s.toString())<0) {
                        etDo.setText("");
                        etDo.requestFocus();
                        Toast.makeText(getApplicationContext(), "DO is invalid", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    etDo.setText("");
                    etDo.requestFocus();
                    Toast.makeText(getApplicationContext(), "DO cannot be negative value", Toast.LENGTH_LONG).show();
                }

            }
        }

        else if(etTds.getText().hashCode() == s.hashCode()) {
            if(s.toString().length()>0)
            {
                try {
                    if(Double.parseDouble(s.toString())>1 && Double.parseDouble(s.toString())<10) {
                        warningAlert(etTds);
                    }
                    else if(Double.parseDouble(s.toString())>10 || Double.parseDouble(s.toString())<0) {
                        etTds.setText("");
                        etTds.requestFocus();
                        Toast.makeText(getApplicationContext(), "TDS is invalid", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    etTds.setText("");
                    etTds.requestFocus();
                    Toast.makeText(getApplicationContext(), "TDS cannot be negative value", Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    //set Google map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");
        mMap = googleMap;
        setDefaultLocation();

        //permission for location
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(mLayout, "You need location access to run this app.",
                        Snackbar.LENGTH_INDEFINITE).setAction("Confirm", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick :");
            }
        });
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                String markerTitle = getCurrentAddress(currentPosition);
                String markerSnippet = "Latitude:" + String.valueOf(location.getLatitude())
                        + " longitude:" + String.valueOf(location.getLongitude());

                Log.d(TAG, "onLocationResult : " + markerSnippet);
                setCurrentLocation(location, markerTitle, markerSnippet);

                mCurrentLocation = location;
            }
        }
    };

    private void startLocationUpdates() {
        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "startLocationUpdates : No permission");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        if (checkPermission()) {
            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            if (mMap != null)
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public String getCurrentAddress(LatLng latlng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        //List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //Toast.makeText(this, "Geocoder is not working", Toast.LENGTH_LONG).show();
            return "Geocoder is not working";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "Wrong GPS", Toast.LENGTH_LONG).show();
            return "Wrong GPS";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "Missing address", Toast.LENGTH_LONG).show();
            return "Missing address";
        } else {
            address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        if (currentMarker != null) currentMarker.remove();
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);
    }

    public void setDefaultLocation() {
        LatLng DEFAULT_LOCATION = new LatLng(-35.238183, 149.084445);
        String markerTitle = "Missing location";
        String markerSnippet = "Please check your permission for location and GPS";

        if (currentMarker != null) currentMarker.remove();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);
    }

    private boolean checkPermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                startLocationUpdates();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "Permission is denied. Please run the app again to allow permission.",
                            Snackbar.LENGTH_INDEFINITE).setAction("Confirm", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();
                } else {
                    Snackbar.make(mLayout, "Permission is denied. Permissions must be allowed in Settings (app info).",
                            Snackbar.LENGTH_INDEFINITE).setAction("Confirm", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();
                }
            }
        }
    }

    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Disable location service");
        builder.setMessage("Location services are required to use the app.\n"
                + "Would you like to edit your location settings?");
        builder.setCancelable(true);
        builder.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    //Convert bitmap to uri
    private Uri getImageUri(Context context, Bitmap photo) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), photo, etSiteCode.getText().toString(), null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : GPS enabled");
                        needRequest = true;
                        return;
                    }
                }
                break;
            case CAMERA_REQUEST_CODE:
                if(resultCode == RESULT_OK)
                {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if(imageBitmap!=null)
                    {
                        mProgress.setMessage("Saving ...");
                        mProgress.show();
                        uriPhotos = getImageUri(this,imageBitmap);
                        if(uriPhotos !=null) {
                            if(imageCount<MAX_IMAGE)
                            {
                                StorageReference filepath = mStorageRef.child(etSiteCode.getText().toString()).child("photo "+imageCount);
                                filepath.putFile(uriPhotos).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        mProgress.dismiss();
                                        Toast.makeText(getApplicationContext(), "Uploading finished ...", Toast.LENGTH_LONG).show();
                                        imageCount++;
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mProgress.dismiss();
                                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else{
                                mProgress.dismiss();
                                Toast.makeText(getApplicationContext(), "Sorry. Up to 5 photos can be taken", Toast.LENGTH_LONG).show();
                            }

                        }
                    }

                }
        }
    }

}

