package com.example.myapplication;

// Bibliotheken werden importiert

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

//Hauptaktivitaet der App
public class MainActivity<runnable> extends AppCompatActivity implements SensorEventListener {

    //Globale Variablen werden festgelegt
    private TextView pressure;
    private TextView elevation;
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private Boolean isPressureSensorAvailable;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private RequestQueue mRequestQueue;
    private TextView textLat;
    private TextView textLong;
    private static final int REQUEST_LOCATION = 1;
    private TextView showLocation;
    TextView textView;
    TelephonyManager telephonyManager;
    String mcc; //Mobile Country Code
    String mnc; //mobile network code
    String cellid; //Cell ID
    String lac; //Location Area Code
    Runnable runnable;
    Handler handler;
    Boolean error;
    String strURLSent;
    String GetOpenCellID_fullresult;
    OpenCellID openCellID;

    // Funktion um Daten von Opencellid.org zu erhalten
    public static class OpenCellID {
        //Interne Variablen festlegen
        String mcc; //Mobile Country Code
        String mnc; //mobile network code
        String cellid; //Cell ID
        String lac; //Location Area Code
        Boolean error;
        String strURLSent;
        String GetOpenCellID_fullresult;
        String latitude;
        String longitude;

        public Boolean isError() {
            return error;
        }

        public void setMcc(String value) {
            mcc = value;
        }

        public void setMnc(String value) {
            mnc = value;
        }

        public void setCallID(int value) {
            cellid = String.valueOf(value);
        }

        public void setCallLac(int value) {
            lac = String.valueOf(value);
        }

        public String getLocation() {
            return (latitude + " : " + longitude);
        }

        //Funktionen um Anfrage an Opencellid.org zu schicken und Ergebniss bearbeiten
        public void groupURLSent() {
            strURLSent = "http://www.opencellid.org/cell/get?key=f3433f6d33be88" + "&mcc=" + mcc + "&mnc=" + mnc + "&lac=" + lac + "&cellid=" + cellid + "&format=json";
        }

        public String getsURLSent() {
            return strURLSent;
        }

        public String getGetOpenCellID_fullresult() {
            return GetOpenCellID_fullresult;
        }

        public void GetOpenCellID() throws Exception {
            groupURLSent();
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(strURLSent);
            HttpResponse response = client.execute(request);
            GetOpenCellID_fullresult = EntityUtils.toString(response.getEntity());
            splitResult();
        }

        //Ergebniss bearbeiten
        private void splitResult() {
            if (GetOpenCellID_fullresult.equalsIgnoreCase("err")) {
                error = true;
            } else {
                error = false;
                String[] tResult = GetOpenCellID_fullresult.split(",");
                latitude = tResult[0];
                longitude = tResult[1];
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Mögliche beschänkungen der App aushebeln
        StrictMode.ThreadPolicy policy = new
        StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Interne Variablen festlegen
        final android.os.Handler customHandler = new android.os.Handler();
        pressure = findViewById(R.id.pressure);
        elevation = findViewById(R.id.elevation);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        textView = findViewById(R.id.text);
        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mRequestQueue = Volley.newRequestQueue(this);
        textView = (TextView) findViewById(R.id.showLocation);

        //Anfrage ob Drucksensor funktionier oder nicht
        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)!= null){
            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            isPressureSensorAvailable = true;
        }else {
            textView.setText("Pressure Sensor not available.");
            isPressureSensorAvailable = false;
        }

        //Handler für eine sich widerhohlende Aufgabe erstellen
        handler = new Handler();
        //Falls Berechtigungen schon freigeben sind mache...
                if (ActivityCompat.checkSelfPermission(MainActivity.this, READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_PHONE_NUMBERS, READ_PHONE_STATE,ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }else {
                    //mache: Starte eine sich wiederhohlende Funktion mit Delay
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            //Interne Variablen festlegen um sie an das Textfelder des Layout zu senden
                            TextView textGsmCellLocation = (TextView)findViewById(R.id.text);
                            TextView textMCC = (TextView)findViewById(R.id.mcc);
                            TextView textMNC = (TextView)findViewById(R.id.mnc);
                            TextView textCID = (TextView)findViewById(R.id.cid);
                            TextView textLAC = (TextView)findViewById(R.id.lac);
                            TextView textGeo = (TextView)findViewById(R.id.geo);
                            TextView textRemark = (TextView)findViewById(R.id.remark);
                            TextView textbsid = (TextView)findViewById(R.id.bsid);
                            TextView textsignal = (TextView)findViewById(R.id.wifisignal);

                            GsmCellLocation cellLocation = (GsmCellLocation)telephonyManager.getCellLocation(); // Funktion um Cell Id, mcc, mnc, lac zu erhalten

                            String networkOperator = telephonyManager.getNetworkOperator();
                            String mcc = networkOperator.substring(0, 3); //Erhalte mcc aus dem String des Telephonymaneger.getcelllocation
                            String mnc = networkOperator.substring(3); //Erhalte mnc aus dem String des Telephonymaneger.getcelllocation
                            textMCC.setText("mcc: " + mcc);
                            textMNC.setText("mnc: " + mnc);
                            int cid = cellLocation.getCid(); //Erhalte Cell Id
                            int lac = cellLocation.getLac(); //Erhalte LAC
                            textGsmCellLocation.setText(cellLocation.toString());
                            textCID.setText("gsm cell id: " + String.valueOf(cid)); //Cell Id an Textfeld im schicken
                            textLAC.setText("gsm location area code: " + String.valueOf(lac)); //Lac an Textfeld im schicken

                            WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE); //wifi service starten um wifi daten zu erhalten
                            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                            textbsid.setText("WIFI BSID: " + wifiInfo.getBSSID()); //bsid an Textfeld im schicken
                            textsignal.setText("Signalstrength: " + wifiInfo.getRssi()); //rssi an Textfeld im schicken

                            //Anfrage ob Drucksensor funktionier oder nicht
                            if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)!= null){
                                pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                                isPressureSensorAvailable = true;
                            }else {
                                textView.setText("Pressure Sensor not available.");
                                isPressureSensorAvailable = false;
                            }

                            //Variablen mit Inhalt füllen
                            openCellID = new OpenCellID();
                            openCellID.setMcc(mcc);
                            openCellID.setMnc(mnc);
                            openCellID.setCallID(cid);
                            openCellID.setCallLac(lac);
                            //versuche eine Anfrage an Opencellid.org zu senden
                            try {
                                openCellID.GetOpenCellID();

                                if(!openCellID.isError()){
                                    textGeo.setText(openCellID.getLocation());
                                    textRemark.setText( "\n\n" + "URL sent: \n" + openCellID.getsURLSent() + "\n\n" + "response: \n" + openCellID.GetOpenCellID_fullresult);
                                }else{
                                    //falls es nicht geht gebe Fehlermeldung raus
                                    textGeo.setText("Error");
                                }
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                textGeo.setText("Exception: " + e.toString());
                            }
                            //Speichere Daten auf internen Speicher
                            File file = new File(getExternalFilesDir(null), "stats.txt");
                            //Interne Variablen festlegen um Daten aus Textfeldern des Layout zu erhalten
                            TextView textViewressure = (TextView) pressure.findViewById(R.id.pressure);
                            TextView textViewelevation = (TextView) elevation.findViewById(R.id.elevation);
                            TextView textViewgeo = (TextView) textGeo.findViewById(R.id.geo);
                            TextView textViewbsid = (TextView) textbsid.findViewById(R.id.bsid);
                            TextView textViewsignal = (TextView) textsignal.findViewById(R.id.wifisignal);

                            //Daten der Textfelder hohlen und in String umwandeln
                            String textpressure = textViewressure.getText().toString();
                            String textelevation = textViewelevation.getText().toString();
                            String textgeolocation = textViewgeo.getText().toString();
                            String textbaseid = textViewbsid.getText().toString();
                            String textsignalstrength = textViewsignal.getText().toString();

                            // Was gespeichert werden soll festlegen
                            String data=  textpressure + "," + textelevation + "," + "lac:" + lac + "," + "mcc:" + mcc + "," + "mnc:" + mnc + "," + "cid:" + cid + "," + textbaseid + "," + textsignalstrength + "," + textgeolocation;

                            //versuche zu speichern
                            try
                            {
                                BufferedWriter buf = new BufferedWriter(new FileWriter(file,true)); // Variable für bufferedwriter festlegen und mit true sagen das Apend gilt
                                buf.append(data); //Daten sschreiben
                                buf.newLine(); // nach jedem Datensatz neue Zeile anfangen
                                buf.close(); // Datei schließen
                            }
                            catch (IOException e)
                            {
                                // TODO Auto-generated catch block
                                //falls Fehler zeige Fehlermeldung
                                e.printStackTrace();
                            }

                            // Widerholungsdauer festlegen
                            handler.postDelayed(this,250);

                        }
                    };
                    // Widerholungsdauer festlegen
                    handler.postDelayed(runnable, 250);
                }
        };

    //Funktion wenn Drucksensor etwas macht
    @Override
    public void onSensorChanged(SensorEvent event) {
        pressure.setText("Druck " + event.values[0] + "hPa"); // Werte des Drucksensors an Textfeld in Layout senden
        double height = (288.15/0.0065 )*(1-Math.pow(event.values[0]/1013.25,1/5.255)); // Druck mittels internationaler Barometricher Höhenformel umrechnen
        elevation.setText(String.format("Höhe über NN %.2f m" , height)); // Höhe im Layout ausgeben
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isPressureSensorAvailable)
        {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    } // Sensor starten und seine Geschwindigkeit zu messen festlegen

    @Override
    protected void onPause() {
        super.onPause();
        if(isPressureSensorAvailable)
        {
            sensorManager.unregisterListener(this);
        }
    } // Sensor bei Pause Ausschalten

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    // Nach Rechten fragen
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (ActivityCompat.checkSelfPermission(this, READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    // Was machen wenn nach Rechten gefragt wird
                    //Starte eine sich wiederhohlende Funktion mit Delay
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            //Interne Variablen festlegen um sie an das Textfelder des Layout zu senden
                            TextView textGsmCellLocation = (TextView)findViewById(R.id.text);
                            TextView textMCC = (TextView)findViewById(R.id.mcc);
                            TextView textMNC = (TextView)findViewById(R.id.mnc);
                            TextView textCID = (TextView)findViewById(R.id.cid);
                            TextView textLAC = (TextView)findViewById(R.id.lac);
                            TextView textGeo = (TextView)findViewById(R.id.geo);
                            TextView textRemark = (TextView)findViewById(R.id.remark);
                            TextView textbsid = (TextView)findViewById(R.id.bsid);
                            TextView textsignal = (TextView)findViewById(R.id.wifisignal);
                            GsmCellLocation cellLocation = (GsmCellLocation)telephonyManager.getCellLocation(); // Funktion um Cell Id, mcc, mnc, lac zu erhalten

                            String networkOperator = telephonyManager.getNetworkOperator();
                            String mcc = networkOperator.substring(0, 3); //Erhalte mcc aus dem String des Telephonymaneger.getcelllocation
                            String mnc = networkOperator.substring(3); //Erhalte mnc aus dem String des Telephonymaneger.getcelllocation
                            textMCC.setText("mcc: " + mcc);
                            textMNC.setText("mnc: " + mnc);
                            int cid = cellLocation.getCid(); //Erhalte Cell Id
                            int lac = cellLocation.getLac(); //Erhalte Lac
                            textGsmCellLocation.setText(cellLocation.toString());
                            textCID.setText("gsm cell id: " + String.valueOf(cid)); //Cell Id an Textfeld im schicken
                            textLAC.setText("gsm location area code: " + String.valueOf(lac)); //LaC an Textfeld im schicken

                            WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE); //wifi service starten um wifi daten zu erhalten
                            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                            textbsid.setText("WIFI BSID: " + wifiInfo.getBSSID()); //bsid an Textfeld im schicken
                            textsignal.setText("Signalstrength: " + wifiInfo.getRssi()); //rssi an Textfeld im schicken

                            //Anfrage ob Drucksensor funktionier oder nicht
                            if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)!= null){
                                pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                                isPressureSensorAvailable = true;
                            }else {
                                textView.setText("Pressure Sensor not available.");
                                isPressureSensorAvailable = false;
                            }

                            //Variablen mit Inhalt füllen
                            openCellID = new OpenCellID();
                            openCellID.setMcc(mcc);
                            openCellID.setMnc(mnc);
                            openCellID.setCallID(cid);
                            openCellID.setCallLac(lac);

                            //versuche eine Anfrage an Opencellid.org zu senden
                            try {
                                openCellID.GetOpenCellID();

                                if(!openCellID.isError()){
                                    textGeo.setText(openCellID.getLocation());
                                    textRemark.setText( "\n\n"
                                            + "URL sent: \n" + openCellID.getsURLSent() + "\n\n"
                                            + "response: \n" + openCellID.GetOpenCellID_fullresult);
                                }else{
                                    //falls es nicht geht gebe Fehlermeldung raus
                                    textGeo.setText("Error");
                                }
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                textGeo.setText("Exception: " + e.toString());
                            }
                            File file = new File(getExternalFilesDir(null), "stats.txt"); //Speichere Daten auf internen Speicher
                            //Interne Variablen festlegen um Daten aus Textfeldern des Layout zu erhalten
                            TextView textViewressure = (TextView) pressure.findViewById(R.id.pressure);
                            TextView textViewelevation = (TextView) elevation.findViewById(R.id.elevation);
                            TextView textViewgeo = (TextView) textGeo.findViewById(R.id.geo);
                            TextView textViewbsid = (TextView) textbsid.findViewById(R.id.bsid);
                            TextView textViewsignal = (TextView) textsignal.findViewById(R.id.wifisignal);

                            //Daten der Textfelder hohlen und in String umwandeln
                            String textpressure = textViewressure.getText().toString();
                            String textelevation = textViewelevation.getText().toString();
                            String textgeolocation = textViewgeo.getText().toString();
                            String textbaseid = textViewbsid.getText().toString();
                            String textsignalstrength = textViewsignal.getText().toString();

                            // Was gespeichert werden soll festlegen
                            String data=  textpressure + "," + textelevation + "," + "lac:" + lac + "," + "mcc:" + mcc + "," + "mnc:" + mnc + "," + "cid:" + cid + "," + textbaseid + "," + textsignalstrength + "," + textgeolocation;

                            //versuche zu speichern
                            try
                            {
                                BufferedWriter buf = new BufferedWriter(new FileWriter(file,true)); // Variable für bufferedwriter festlegen und mit true sagen das Apend gilt
                                buf.append(data); //Daten sschreiben
                                buf.newLine(); // nach jedem Datensatz neue Zeile anfangen
                                buf.close(); // Datei schließen
                            }
                            catch (IOException e)
                            {
                                // TODO Auto-generated catch block
                                //falls Fehler zeige Fehlermeldung
                                e.printStackTrace();
                            }
                            // Widerholungsdauer festlegen
                            handler.postDelayed(this,250);

                        }
                    };
                    // Widerholungsdauer festlegen
                    handler.postDelayed(runnable, 250);
                }
        }
    }
};






