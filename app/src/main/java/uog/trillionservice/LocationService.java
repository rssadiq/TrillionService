package uog.trillionservice;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.TextView;
import android.app.Activity;

import modules.*;

/**
 * Created by Sadiq on 25/04/2017.
 */

public class LocationService extends Service implements IModuleSubscriber{

    //creating a mediaplayer object
//    private MediaPlayer player;

    private LocationModule locationModule;

//    MainActivity mainActivity= new MainActivity();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationModule = new LocationModule(this);
        locationModule.addSubscriber(this);
        beginModule(locationModule);

        // When the parent service is Forced to Stop, everything stops immediately.
        // So, the Log files are not closed properly and they are not vissible from
        // the PC because the MediaScannerConnection.scanFile is not executed.
        // We're running this command MediaScannerConnection.scanFile here so that
        // it makes those files available when the service restarts.
        MediaScannerConnection.scanFile(this,new String[]{"/storage/emulated/0/Trillion/Location_Module"},null,null);

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }


    @Override
    public void onDestroy() {

        super.onDestroy();
        endModule(locationModule);
        //stopping the player when service is destroyed
        //player.stop();

/*        endModule(usageModule);
        endModule(wiFiModule);
        */

    }
    private void beginModule(Module module){
        if(module.isRunning()){ // If the module is running, stop it
            module.stopModule();
        }
        // Set parameters to the module and start
        module.setNumOfRuns(50);
        module.setSleepTime(3000);
        module.setRunMode(ModuleRunModes.CONTINUOUS_RUN_MODE);
        module.startModule();
    }
    /**
     * Stops / Ends a module. If the module is in running state
     * @param module The module to be stoped
     */
    private void endModule(Module module){
        if(module.isRunning()){ // If the module is running, stop it
            module.stopModule();
        }
    }


    @Override
    public void moduleStarted(Module module) {

    }

    @Override
    public void moduleUpdate(Module module, String message) {

    }

    @Override
    public void moduleComplete(Module module) {

    }
}
