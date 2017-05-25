package modules;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;

import uog.trillionservice.MainActivity;
import uog.trillionservice.R;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by sr3897c on 04-05-17.
 */

public class DataProcessor {

    private static final String MODULE_NAME = "Location_Module", TAG = "DATA PROCESSOR MODULE";
    private static String lastSatPRN = "", lastSatSNR = "", lastUnusedSatPRNs = "", lastUnusedSatSNRs = "";
    private static float lastCPUusage = 0, lastLocAccuracy = 0;
    private String satPrn = "", satSnr = "", unusedSatPRNs = "", unusedSatSNRs = "";
    private double lastLatitude = 0, lastLongitude = 0; //, latitudeAvg = 0, longitudeAvg = 0, accuracyAvg = 0;
    private float accuracy = 0;     //, minSNR = 0, maxSNR = 0, rangeSNR = 0, avgSNR = 0, stdDevSNR = 0;
    boolean locationChanged = false, accuracyChanged = false, satPRNsChanged = false, satSNRsChanged = false, unusedSatPRNsChanged = false, unusedSatSNRsChanged = false;
    private Context context;
    public double trustScore=0.0;

    protected DataProcessor(Context context) {
        this.context = context;
//        Toast.makeText(context, "Service Started", Toast.LENGTH_SHORT).show();
    }

    protected void dataSample(float cpuUsage, double latitude, double longitude, float accuracy, int noOfSatUsed, String satPrn, String satSnr, String unusedSatPRNs, String unusedSatSNRs) {

        Log.d(TAG, "DATA PROCESSOR OUTPUT - 1 : \n CPU Usage: " + Float.toString(cpuUsage) + " - Last CPU Usage" + Float.toString(lastCPUusage) +
                ",\n Latitude: " + Double.toString(latitude) + " - Last Lat" + Double.toString(lastLatitude) +
                ",\n Longitude: " + Double.toString(longitude) + " - Last Long" + Double.toString(lastLongitude) +
                ",\n Accuracy: " + Float.toString(accuracy) + " - Last Accuracy: " + Float.toString(lastLocAccuracy) +
                ",\n satellites: " + satPrn + " - Last Satellites: " + lastSatPRN +
                ",\n SNR: " + satSnr + " - Last SNRs: " + lastSatSNR +
                ",\n Unused Satellites: " + unusedSatPRNs + " - Last Unused Satellites: " + lastUnusedSatPRNs +
                ",\n Unused SNRs: " + unusedSatSNRs + " - Last Unused SNRs: " + lastUnusedSatSNRs);

        if (lastLatitude != 0 && lastLongitude != 0) {
            if (lastLatitude == latitude && lastLongitude == longitude) {
                locationChanged = false;
            } else
                locationChanged = true;
        }
        if (lastLocAccuracy != 0) {
            if (lastLocAccuracy == accuracy) {
                accuracyChanged = false;
            } else
                accuracyChanged = true;
        }

        if (lastSatPRN != "" && lastSatSNR != "") {
            if (lastSatPRN == satPrn) {
                satPRNsChanged = false;
            } else
                satPRNsChanged = true;
            if (lastSatSNR == satSnr) satSNRsChanged = false;
            else
                satSNRsChanged = true;
        }

        if (lastUnusedSatPRNs != "" && lastUnusedSatSNRs != "") {


            if (lastUnusedSatPRNs == unusedSatPRNs) unusedSatPRNsChanged = false;
            else
                unusedSatPRNsChanged = true;

            if (lastUnusedSatSNRs == unusedSatSNRs) unusedSatSNRsChanged = false;
            else
                unusedSatSNRsChanged = true;
        }

        Log.d(TAG, "DATA PROCESSOR OUTPUT - 2:\n " +
//                "\n CPU Usage: " + Float.toString(cpuUsage) +
//                ", Latitude:"  + Double.toString(latitude) +
//                ", Longitude: " + Double.toString(longitude) +
//                ", Accuracy: " + Float.toString(accuracy) +
                ", locationChanged: " + Boolean.toString(locationChanged) +
                ", accuracyChanged: " + Boolean.toString(accuracyChanged) +
                ", satPRNsChanged: " + Boolean.toString(satPRNsChanged) +
                ", satSNRsChanged: " + Boolean.toString(satSNRsChanged) +
                ", unusedSatPRNsChanged: " + Boolean.toString(unusedSatPRNsChanged) +
                ", unusedSatSNRsChanged: " + Boolean.toString(unusedSatSNRsChanged));

//        if (locationChanged && accuracyChanged && satPRNsChanged && satSNRsChanged && unusedSatPRNsChanged && unusedSatSNRsChanged) {
//            showNotification("All LOCATION parameters CHANGEd");
//        } else {
////            Log.d(TAG, "DATA PROCESSOR OUTPUT: All LOCATION parameters CHANGEd \n");
//            showNotification("One or more  location parameters are UNchanced");
//        }
//                Toast.makeText(context, "Suspicious", Toast.LENGTH_LONG).show();

        int acrcyChanged = (accuracyChanged && noOfSatUsed >=3) ? 1 : 0;
        int snrChanged = (satSNRsChanged && noOfSatUsed >=3) ? 1 : 0;
        Logit logisticRegression = new Logit(cpuUsage, acrcyChanged, snrChanged);
        trustScore = logisticRegression.probability; //This is the Probability of Normality
        showNotification(Double.toString(trustScore));


        //region SET CURRENT VALUES AS LAST VALUES
        lastCPUusage = cpuUsage;
        lastLatitude = latitude;
        lastLongitude = longitude;
        lastLocAccuracy = accuracy;
        lastSatPRN = satPrn;
        lastSatSNR = satSnr;
        lastUnusedSatPRNs = unusedSatPRNs;
        lastUnusedSatSNRs = unusedSatSNRs;

        //showNotification();
        //endregion
    }

    protected void showNotification(String msg) {

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);//(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification myNotication = new Notification();
        //API level 11
        Intent intent = new Intent("com.rj.notitfications.SECACTIVITY");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, 0);

        Notification.Builder builder = new Notification.Builder(context);

        builder.setAutoCancel(false);
        builder.setTicker("this is ticker text");
        builder.setContentTitle("Trustworthiness Score:");
        builder.setContentText(msg);
//        builder.setContentText("It's a test notification from TRILLION SERVICE");
        if(Float.valueOf(msg)>0.96)
            builder.setSmallIcon(R.drawable.safe);//(R.drawable.ic_launcher);
        else if(Float.valueOf(msg)<=0.96 && Float.valueOf(msg)>0.886370624)
            builder.setSmallIcon(R.drawable.safe2);//(R.drawable.ic_launcher);
        else if(Float.valueOf(msg)<=0.886370624 && Float.valueOf(msg)>0.8415)
            builder.setSmallIcon(R.drawable.unsafe2);//(R.drawable.ic_launcher);
         else   builder.setSmallIcon(R.drawable.dangerous);
//        builder.setSmallIcon(R.drawable.ic_launcher);//android.R.drawable.stat_notify_more);//(
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setSubText("TRILLION Notification");   //API level 16
        builder.setNumber(100);
        builder.build();

        myNotication = builder.getNotification();
        manager.notify(11, myNotication);

    }
//    protected void showNotification2(){
//        Notification notification = new Notification(android.R.drawable.stat_notify_more,
//                R.string.name_of_your_app,
//                System.currentTimeMillis());
////        Notification notification = new Notification(R.drawable.your_app_icon,
////                R.string.name_of_your_app,
////                System.currentTimeMillis());
//        notification.flags |= Notification.FLAG_NO_CLEAR
//                | Notification.FLAG_ONGOING_EVENT;
//        NotificationManager notifier = (NotificationManager)
//                context.getSystemService(Context.NOTIFICATION_SERVICE);
//        notifier.notify(1, notification);
//    }
}


