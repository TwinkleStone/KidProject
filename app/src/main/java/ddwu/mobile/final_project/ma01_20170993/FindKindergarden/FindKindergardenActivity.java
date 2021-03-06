package ddwu.mobile.final_project.ma01_20170993.FindKindergarden;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import ddwu.mobile.final_project.ma01_20170993.R;

public class FindKindergardenActivity extends AppCompatActivity implements Button.OnTouchListener{

    private FragmentManager fragmentManager = getSupportFragmentManager();
    EditText etTarget;
    LinearLayout laySearch;

    private GpsTracker gpsTracker;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    double latitude;
    double longitude;

    String apiAddress;
    String query;
    KindergardenAdapter adapter;
    ArrayList<KindergardenDto> resultList;
    KindergardenFindXmlParser parser;
    ListView lvList;
    TextView tvNoFind;
    TextView tvIsFind;
    TextView tvGuide;
    InputMethodManager imm;
    Button btnSearch;
    Button ShowLocationButton;
    TextView textview_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_kindergarden);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        textview_address = (TextView)findViewById(R.id.textview);
        ShowLocationButton = (Button) findViewById(R.id.button);
        ShowLocationButton.setOnTouchListener(this);

        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnTouchListener(this);
        etTarget = (EditText) findViewById(R.id.etTarget);
        laySearch = (LinearLayout) findViewById(R.id.laySearch);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        etTarget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(etTarget.getText().toString().equals("")){
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    TabFragment1 fragmentHome = new TabFragment1("0", -1);
                    transaction.replace(R.id.tab1, fragmentHome).commitAllowingStateLoss();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // ??????
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec ts1 = tabHost.newTabSpec("Tab Spec 1");
        ts1.setContent(R.id.tab1);
        ts1.setIndicator("?????????");
        tabHost.addTab(ts1);
        TabHost.TabSpec ts2 = tabHost.newTabSpec("Tab Spec 2");
        ts2.setContent(R.id.tab2);
        ts2.setIndicator("?????????");
        tabHost.addTab(ts2);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        TabFragment1 fragmentHome = new TabFragment1("0", -1);
        transaction.replace(R.id.tab1, fragmentHome).commitAllowingStateLoss();

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                imm.hideSoftInputFromWindow(etTarget.getWindowToken(), 0);
                Log.i("TAB CHANGED", tabId);
                if(tabId.equals("Tab Spec 2")){
                    laySearch.setVisibility(View.GONE);
                }else{
                    laySearch.setVisibility(View.VISIBLE);
                }
            }
        });

        lvList = (ListView) findViewById(R.id.lvList);
        tvNoFind = (TextView) findViewById(R.id.tvNoFind);
        tvIsFind = (TextView) findViewById(R.id.tvIsFind);
        tvGuide = (TextView) findViewById(R.id.tvGuide);

        resultList = new ArrayList();
        adapter = new KindergardenAdapter(this, R.layout.custom_listview_kindergarden, resultList);
        lvList.setAdapter(adapter);
        parser = new KindergardenFindXmlParser();

        lvList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isOnline()) {
                    Intent intent = new Intent(FindKindergardenActivity.this, KindergardenViewActivity.class);
                    intent.putExtra("kindergarden", resultList.get(position));
                    startActivity(intent);
                }else{
                    Toast.makeText(FindKindergardenActivity.this, "???????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
     * ActivityCompat.requestPermissions??? ????????? ????????? ????????? ????????? ???????????? ??????????????????.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????
            boolean check_result = true;

            // ?????? ???????????? ??????????????? ???????????????.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if ( check_result ) {//?????? ?????? ????????? ??? ??????
            } else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ????????? ??????????????? ?????? ???????????????.2 ?????? ????????? ????????????.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(FindKindergardenActivity.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.", Toast.LENGTH_LONG).show();
                    finish();
                }else {
                    Toast.makeText(FindKindergardenActivity.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission(){
        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(FindKindergardenActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(FindKindergardenActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)
            // 3.  ?????? ?????? ????????? ??? ??????
        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.
            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(FindKindergardenActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(FindKindergardenActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(FindKindergardenActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(FindKindergardenActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    public String getCurrentAddress( double latitude, double longitude) {
        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(FindKindergardenActivity.this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }
        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????!\n?????????????????? ??????????????? ??????????????????.", Toast.LENGTH_LONG).show();
            return "?????? ?????????";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";
    }

    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FindKindergardenActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void hideKeyboard(){
        imm.hideSoftInputFromWindow(etTarget.getWindowToken(), 0);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Button i = null;
        int tmp = -1;
        switch (v.getId()) {
            case R.id.btnSearch:
                i = btnSearch;
                tmp = 0;
                break;
            case R.id.button:
                i = ShowLocationButton;
                tmp = 1;
                break;
        }
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                i.getBackground().setColorFilter(0x66ffffff, PorterDuff.Mode.SRC_ATOP);
                break;
            case MotionEvent.ACTION_UP:
                i.getBackground().clearColorFilter();
                if(tmp == 0) {
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    imm.hideSoftInputFromWindow(etTarget.getWindowToken(), 0);
                    if(etTarget.getText().toString().equals("")){
                        Toast.makeText(FindKindergardenActivity.this, "?????????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                    }else {
                        FindFragment findFragment = new FindFragment();
                        findFragment.setEtTarget(etTarget.getText().toString());
                        transaction.replace(R.id.tab1, findFragment).commitAllowingStateLoss();
                    }
                }else if(tmp == 1){
                    tvGuide.setVisibility(View.GONE);
                    gpsTracker = new GpsTracker(FindKindergardenActivity.this);
                    latitude = gpsTracker.getLatitude();
                    longitude = gpsTracker.getLongitude();

                    String address = getCurrentAddress(latitude, longitude);
                    textview_address.setText(address);

                    if(address.length() >= 14 && address.substring(0, 11).equals("???????????? ??????????????? ")){
                        query = " / / /" + address.substring(11, 14);
                    }else if(!address.equals("?????? ?????????")){
                        Toast.makeText(FindKindergardenActivity.this, "???????????? ?????? ??????????????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
                        query = " / / /?????????";
                    }else{
                        query = " / / /?????????";
                    }
                    apiAddress = getResources().getString(R.string.api_url3);
                    if(isOnline()) {
                        try {
                            new KindergardenLocationAsyncTask().execute(apiAddress + URLEncoder.encode(query, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(FindKindergardenActivity.this);
                        builder.setTitle("????????? ??????");
                        builder.setMessage("???????????? ?????? ??? ????????? ???????????????.");
                        builder.setCancelable(false);
                        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                Boolean wantToCloseDialog = isOnline();
                                if(wantToCloseDialog) {
                                    dialog.dismiss();
                                    try {
                                        new KindergardenLocationAsyncTask().execute(apiAddress + URLEncoder.encode(query, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
    //                Toast.makeText(FindKindergardenActivity.this, "???????????? \n?????? " + latitude + "\n?????? " + longitude, Toast.LENGTH_LONG).show();
                }
                break;
        }
        return true;
    }

    class KindergardenLocationAsyncTask extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDlg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDlg = ProgressDialog.show(FindKindergardenActivity.this, "Wait", "Downloading...");
        }

        @Override
        protected String doInBackground(String... strings) {
            String address = strings[0];
            String result = downloadContents(address);
            if (result == null) return "Error!";
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            resultList = parser.parse(result);      // ?????? ??????

            double la;
            double lo;

            if(longitude != 0.0 || latitude != 0.0) {
                for (int i = 0; i < resultList.size(); i++) {
                    la = resultList.get(i).getLatitude();
                    lo = resultList.get(i).getLongitude();
                    double diff = getDistance(la, lo, latitude, longitude);
                    if (diff <= 500) {
                        continue;
                    } else {
                        resultList.remove(i);
                        i--;
                    }
                }
            }

            adapter.setList(resultList);    // Adapter ??? ?????? ????????? ?????? ?????? ArrayList ??? ??????
            adapter.notifyDataSetChanged();

            tvNoFind.setText("???????????? " + resultList.size() + "???");
            if(resultList.size() == 0){
                tvIsFind.setVisibility(View.VISIBLE);
            }
            else{
                tvIsFind.setVisibility(View.GONE);
            }

            progressDlg.dismiss();
        }

        /* URLConnection ??? ???????????? ???????????? ?????? ??? ??????, ?????? ??? ????????? InputStream ?????? */
        private InputStream getNetworkConnection(HttpURLConnection conn) throws Exception {

            // ??????????????? ????????? ??? ????????? ????????? ?????? URL ??????
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(3000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + conn.getResponseCode());
            }

            return conn.getInputStream();
        }

        /* InputStream??? ???????????? ???????????? ?????? ??? ?????? */
        protected String readStreamToString(InputStream stream){
            StringBuilder result = new StringBuilder();

            try {
                InputStreamReader inputStreamReader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String readLine = bufferedReader.readLine();

                while (readLine != null) {
                    result.append(readLine + "\n");
                    readLine = bufferedReader.readLine();
                }

                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result.toString();
        }

        /* ??????(address)??? ???????????? ????????? ???????????? ????????? ??? ?????? */
        protected String downloadContents(String address) {
            HttpURLConnection conn = null;
            InputStream stream = null;
            String result = null;

            try {
                URL url = new URL(address);
                conn = (HttpURLConnection)url.openConnection();
                stream = getNetworkConnection(conn);
                result = readStreamToString(stream);
                if (stream != null) stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }

            return result;
        }

    }

    public double getDistance(double lat1 , double lng1 , double lat2 , double lng2 ){
        double distance;

        Location locationA = new Location("point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lng1);

        Location locationB = new Location("point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lng2);

        distance = locationA.distanceTo(locationB);

        return distance;
    }

    /* ???????????? ?????? ????????? */
    /* ???????????? ?????? ?????? */
    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
