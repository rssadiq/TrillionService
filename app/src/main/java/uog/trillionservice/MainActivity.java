package uog.trillionservice;



import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import modules.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{//, IModuleSubscriber {

    //button objects
    private Button buttonStart;
    private Button buttonStop;

    // Default configs for modules
    private int runs = 1, sleep = 1000, mode = ModuleRunModes.LIMITED_RUN_MODE;
    private String moduleSelected = "";
    // Deceleration of modules
    /*private UsageModule usageModule;
    private WiFiModule wiFiModule;*/
//    public LocationModule locationModule;

    private static final String[] REQUIRED_PERMISSIONS = { // Permissions to prompt user for
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Storage
            Manifest.permission.ACCESS_FINE_LOCATION, // Location
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int PERMISSIONS_REQUEST = 101; // Permission request number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getting buttons from xml
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);

        //attaching onclicklistener to buttons
        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        initialiseGeneral();
        startService(new Intent(this, LocationService.class));
        //Start the app minimised
        minimizeApp();
    }

    @Override
    public void onClick(View view) {
        if (view == buttonStart) {
            //start the service here
            startService(new Intent(this, LocationService.class));
            minimizeApp();

        } else if (view == buttonStop) {
            //stop the service here
            stopService(new Intent(this, LocationService.class));
        }
    }

    //Minimize or Hide the user interface of the App
    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /**
     * Performs the general application specific initialisation
     */
    private void initialiseGeneral() {
        // Check App Permissions
        checkPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST);
/*        // Text Views
        outputTxt = (TextView) findViewById(R.id.outputTxt);
        // Spinner
        moduleSpinner = (Spinner) findViewById(R.id.moduleSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.modulesArray, android.R.layout.simple_spinner_dropdown_item);
        moduleSpinner.setAdapter(adapter);
        moduleSpinner.setOnItemSelectedListener(this);
        // Buttons
        startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(this);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(this);
        // Switches
        continuousSwitch = (Switch) findViewById(R.id.continuousSwitch);
        continuousSwitch.setOnCheckedChangeListener(this);
        // Edit Text Fields
        runsTxt = (EditText) findViewById(R.id.runsTxt);
        runsTxt.addTextChangedListener(this);
        sleepTxt = (EditText) findViewById(R.id.sleepTxt);
        sleepTxt.addTextChangedListener(this);
        // Scroll View
        scrollView = (ScrollView) findViewById(R.id.scollview);
        */

        // Modules
/*      usageModule = new UsageModule(this);
        usageModule.addSubscriber(this);
        wiFiModule = new WiFiModule(this);
        wiFiModule.addSubscriber(this);*/
//        locationModule = new LocationModule(this);
//        locationModule.addSubscriber(this);

    }
    // Called after
    // Adapted From: http://stackoverflow.com/questions/33139754/android-6-0-marshmallow-cannot-write-to-sd-card
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case PERMISSIONS_REQUEST:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    finish();
                    startActivity(getIntent());
                }else{
                    Toast.makeText(this,"The permission(s) are required for the application to function. Please enable the permission(s).",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * Checks if the passed permissions have been granted. If not, will request the permission to be
     * granted from the user.
     * @param permissionsToCheck The permission to check.
     * @param requestNumber The request number associated with permissions to check.
     */
    private void checkPermissions(String[] permissionsToCheck, final int requestNumber){
        List<String> permissionsToGet = new ArrayList<>();
        for(String permission : permissionsToCheck){ // Get permission that are not granted
            boolean granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            if(!granted){ // If the permission is not granted, add to permissions to request for
                permissionsToGet.add(permission);
            }
        }
        if(!permissionsToGet.isEmpty()){ // If there is a permission required to be granted
            String[] permissions = new String[permissionsToGet.size()];
            for(int i = 0; i < permissions.length; i++){ // Copy required permissions list to String array
                permissions[i] = permissionsToGet.get(i);
            }
            ActivityCompat.requestPermissions(this,permissions,requestNumber);
        }
    }
//    /**
//     * Starts / Begins a module with global parameters.
//     * @param module The module to be started.
//     */
//    protected void beginModule(Module module){
//        if(module.isRunning()){ // If the module is running, stop it
//            module.stopModule();
//        }
//        // Set parameters to the module and start
//        module.setNumOfRuns(runs);
//        module.setSleepTime(sleep);
//        module.setRunMode(mode);
//        module.startModule();
//    }
//    /**
//     * Stops / Ends a module. If the module is in running state
//     * @param module The module to be stoped
//     */
//    protected void endModule(Module module){
//        if(module.isRunning()){ // If the module is running, stop it
//            module.stopModule();
//        }
//    }
//
//    @Override
//    public void moduleStarted(Module module) {
//
//    }
//
//    @Override
//    public void moduleUpdate(Module module, String message) {
//
//    }
//
//    @Override
//    public void moduleComplete(Module module) {
//
//    }
}

