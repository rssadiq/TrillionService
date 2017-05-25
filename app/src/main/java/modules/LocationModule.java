package modules;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AndroidException;
import android.util.Log;
import android.widget.Toast;

import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.*;


public class LocationModule extends Module implements LocationListener, GpsStatus.Listener {
//    noOfSatUsed=0,noOfSatUnused=0;
//    float =0;
//    Boolean satellitesChanged =false, unusedSatellitesChanged =false, unusedSNRChanged
//    private UsageModule usageModule= new UsageModule(context);
//    usageModule.addSubscriber(this);
    private float cpuUsage;
    private int numCores;
    private double ram,ratioOfUsedUnusedSat=0.0;
    private static final int CORE_LOAD_WAIT_TIME = 200; // Wait time between cpu core load reads and number of rolling samples to store

    private static final String MODULE_NAME = "Location_Module", TAG = "LOCATION MODULE", MOCK_PROVIDER_NAME = "mock";
    private static final boolean DEBUGGING = true;
    private static final long TIME_BETWEEN_UPDATES = 1000; // Minimum time between location updates
    private static final float DISTANCE_CHANGE_BETWEEN_UPDATES = 5; // Minimum distance change between updates.
    // "Time of Log,Provider,Latitude,Latitude Avg,Longitude,Longitude Avg,Accuracy,Accuracy Avg,Time to First Fix (milliseconds),Satellite IDs (PRNs),SNR,Unused SatPRNs,Unused SatSNRs,Time of Reading SNR,MinSNR,MaxSNR,SNRrange,AvgSNR,StdDevSNR,Epoch Time,Time Date"; //
    private static final String LOG_FILE_HEADER ="Time of Log,Provider,Location Reading Time,Time to First Location Fix (milliseconds),"
+ "Trustworthiness Score (Probability),"
            + "CPU Usage,"
            + "Latitude,"
+ "Longitude,"
+ "Location Changed,"
+ "Accuracy,"
+ "Accuracy Changed,"
+ "No of Satellites Used,"
+ "No of Unused Satellites,"
+ "Ratio of Used and Unused Satellites,"
+ "Satellite IDs (PRNs),"
+ "Satellites Changed,"
+ "SNR,"
+ "SNR Changed,"
+ "Unused SatPRNs,"
+ "Unused SatPRNs Changed,"
+ "Unused SatSNRs,"
+ "unusedSNRChanged,"
+ "Time of Reading SNR,MinSNR,MaxSNR,SNRrange,AvgSNR,StdDevSNR";
//"Time of Log,Provider,Epoch Time,Time Date,CPU Usage,Latitude,Longitude,Accuracy,Time to First Fix (milliseconds),Satellite IDs (PRNs),SNR,Unused SatPRNs,Unused SatSNRs,Time of Reading SNR,MinSNR,MaxSNR,SNRrange,AvgSNR,StdDevSNR";

    //region POSSIBLE UNNECESSARY CODE FOR CURRENT USE - 1

    private static boolean useMockLocations = false; // Debugging and mock locations flags
    private static int NUM_OF_SAMPLES = 10;
    private final double[] MOCK_LOCATION_LATITUDE = {
            -0.0077,
            -1.1581,
            -0.1357,
            0.7077,
            -2.2426,
            -2.9916,
            -3.5339,
            -5.7123,
            1.2974,
            0.1218
    };
    private final double[] MOCK_LOCATION_LONGITUDE = {
            51.4826,
            52.9548,
            51.4975,
            51.5459,
            53.4808,
            53.4084,
            50.7184,
            50.0658,
            52.6309,
            52.2053
    };
    private final float[] MOCK_LOCATION_ACCURACY = {
            10.0F,
            10.1F,
            10.2F,
            10.3F,
            10.4F,
            10.5F,
            10.6F,
            10.7F,
            10.8F,
            10.9F
    };


    //endregion

    private int mockLocCount = 0, time2firstFix = 0, noOfSatUsed=0, noOfSatUnused=0;
    private LocationManager locationManager = null;
    private Location location;
    private SimpleDateFormat simpleDateFormat; // Used to format time of location fix
    private long timeEpoch = 0, timeOfLastFix = 0, timeSinceAccuracyChanged = 0, timeSinceSNRsChanged = 0;
    private String provider = "", time = "", satPrn = "", satSnr = "", snrTimes = "", unusedSatPRNs = "", unusedSatSNRs = "";
    private double latitude = 0, longitude = 0, latitudeAvg = 0, longitudeAvg = 0, accuracyAvg = 0;
    private float accuracy = 0, lastAccuracy=0, minSNR = 0, maxSNR = 0, rangeSNR = 0, avgSNR = 0, stdDevSNR = 0;
    private double[] latitudeSamples = new double[NUM_OF_SAMPLES], longitudeSamples = new double[NUM_OF_SAMPLES];
    private float[] accuracySamples = new float[NUM_OF_SAMPLES];

    //region CONSTRUCTORS
    public LocationModule(Context context) { // Default Constructor
        super(context, MODULE_NAME);
        init();
    }
//
//    public LocationModule(Context context, int runMode) {
//        super(context, MODULE_NAME, runMode);
//        init();
//    }
//
//    public LocationModule(Context context, int runMode, String moduleName) {
//        super(context, moduleName, runMode);
//        init();
//    }
//
//    public LocationModule(Context context, int runMode, String moduleName, int numOfRuns, int sleepTime) {
//        super(context, moduleName, runMode, numOfRuns, sleepTime);
//        init();
//    }
    //endregion

    private static LocationManager getLocationManager(Context context) {
        try {
            return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        } catch (Exception ex) {
            return null;
        }
    }


    //region DETERMINE THE LOCATION PROVIDER
    private static String getCurrentProvider(LocationManager locationManager) {
        if (locationManager != null) {

            if (isGPSEnabled(locationManager)) {
                return LocationManager.GPS_PROVIDER;
            } else if (isNetworkEnabled(locationManager)) {
                Log.d(TAG, "Network Available");
                return locationManager.NETWORK_PROVIDER;
            } else {
                Log.d(TAG, "Network Unavailable");
            }
            // Maybe use locationManager.getBestProvider() here???
        }
        return null;
    }

    private static boolean isGPSEnabled(LocationManager locationManager) {
        try {
            return (locationManager != null) && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isNetworkEnabled(LocationManager locationManager) {
        try {
            return (locationManager != null) && (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        } catch (Exception ex) {
            return false;
        }
    }
    //endregion

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        setProviderInfo(getCurrentProvider(locationManager));
    }

    @Override
    public void onProviderEnabled(String provider) {
        setProviderInfo(getCurrentProvider(locationManager));
    }

    @Override
    public void onProviderDisabled(String provider) {
        setProviderInfo(getCurrentProvider(locationManager));
    }

    public void setMockLocations(boolean mockLocation) {
        useMockLocations = mockLocation;
        init();
    }


    private void getLocationInfo() {
        if (locationManager != null) {
            location = null;
            try {
                setProviderInfo(getCurrentProvider(locationManager));

                //region * GET LOCATION IF(provider != null)
                if (provider != null) {
                    if (useMockLocations && provider.equalsIgnoreCase(MOCK_PROVIDER_NAME)) {
                        location = getMockLocation();
                    } else {
                        location = locationManager.getLastKnownLocation(provider);
                    }
                }
                //endregion

                //region * SET LOCATION INFO
                if (location != null) {
                    if (!(location.getProvider().equals(provider))) {
                        setProviderInfo(location.getProvider());
                    }

                    setLatitudeInfo(location.getLatitude());
                    setLongitudeInfo(location.getLongitude());
                    setAccuracyInfo(location.getAccuracy());
                    setTimeInfo(location.getTime());
                    setTimeInfoUTC(location.getTime());

                } else {
                    Log.d(TAG, "Location is null");
                }
                //endregion

            } catch (java.lang.SecurityException ex) {
                Log.e(TAG, "Location Permission Not Enabled");
            }
        }
    }

    private Location getMockLocation() {
        Location mockLoc = new Location(MOCK_PROVIDER_NAME);
        mockLoc.setLatitude(MOCK_LOCATION_LATITUDE[mockLocCount]);
        mockLoc.setLongitude(MOCK_LOCATION_LONGITUDE[mockLocCount]);
        mockLoc.setAccuracy(MOCK_LOCATION_ACCURACY[mockLocCount]);
        mockLoc.setTime(System.currentTimeMillis());
        mockLocCount++;
        if (mockLocCount >= MOCK_LOCATION_LATITUDE.length) {
            mockLocCount = 0;
        }
        return mockLoc;
    }

    //region UNNECESSARY FOR CURRENT USE - -2

//     @Override
//    protected void collectConstSample() {}
    @Override
    protected void collectConstSample() {
        String stats = "Minimum Time:," + TIME_BETWEEN_UPDATES + " ms" +
                ",Distance Changed:," + DISTANCE_CHANGE_BETWEEN_UPDATES + " m" +
                ",Averages From:," + NUM_OF_SAMPLES + " Samples";
        logger.log(stats, false);
        logger.log(LOG_FILE_HEADER, false);
        if (DEBUGGING) {
            publishModuleUpdate(stats);
            Log.d(TAG, stats);
        }
    }
    //endregion

    //region collectSample VERSION-1
//        @Override
//    protected void collectSample() {
//
//        getLocationInfo();
//        latitudeSamples = ModuleUtilLib.addSample(latitudeSamples, latitude, sampleCount);
//        //latitudeAvg = ModuleUtilLib.calcAverage(latitudeSamples, sampleCount);
//        longitudeSamples = ModuleUtilLib.addSample(longitudeSamples, longitude, sampleCount);
//        //longitudeAvg = ModuleUtilLib.calcAverage(longitudeSamples, sampleCount);
//        accuracySamples = ModuleUtilLib.addSample(accuracySamples, accuracy, sampleCount);
//        //accuracyAvg = ModuleUtilLib.calcAverage(accuracySamples, sampleCount);
//        String stats = provider + "," +
//                Long.toString(timeEpoch) + "," +
//                time + "," +
//                Double.toString(latitude) + "," +
//                //Double.toString(latitudeAvg) + "," +
//                Double.toString(longitude) + "," +
//                //Double.toString(longitudeAvg) + "," +
//                Float.toString(accuracy) + "," +
//                //Double.toString(accuracyAvg) + "," +
//                Integer.toString(time2firstFix) + "," +
//                satPrn + "," +
//                satSnr + "," +
//                unusedSatPRNs + "," +
//                unusedSatSNRs + "," +
//                snrTimes + "," +
//                minSNR + "," +
//                maxSNR + "," +
//                rangeSNR + "," +
//                avgSNR + "," +
//                stdDevSNR;
//
//        logger.log(stats, true);
//        if (DEBUGGING) {
//            publishModuleUpdate(stats);
//            Log.d(TAG, "Provider: " + provider +
//                    ", Latitude: " + Double.toString(latitude) +
//                    ", Latitude Average: " + Double.toString(latitudeAvg) +
//                    ", Longitude: " + Double.toString(longitude) +
//                    ", Longitude Average: " + Double.toString(longitudeAvg) +
//                    ", Accuracy: " + Float.toString(accuracy) +
//                    ", Accuracy Average: " + Double.toString(accuracyAvg) +
//                    ", Epoch Time of Fix: " + timeEpoch +
//                    ", Time of Fix: " + time);
//        }
//    }
    //endregion

    //region collectSample VERSION-2
    @Override
    protected void collectSample() {
        getLocationInfo();

        numCores = UsageModule.getNumCores();
        cpuUsage = UsageModule.getCpuUsage(numCores);
        dataProcessor.dataSample(cpuUsage,latitude,longitude,accuracy,noOfSatUsed,satPrn,satSnr,unusedSatPRNs,unusedSatSNRs);

        ratioOfUsedUnusedSat = (double) noOfSatUsed / noOfSatUnused;

        //ram = getMemoryUsed(context);
//        latitudeSamples = ModuleUtilLib.addSample(latitudeSamples, latitude, sampleCount);
//        //latitudeAvg = ModuleUtilLib.calcAverage(latitudeSamples, sampleCount);
//        longitudeSamples = ModuleUtilLib.addSample(longitudeSamples, longitude, sampleCount);
//        //longitudeAvg = ModuleUtilLib.calcAverage(longitudeSamples, sampleCount);
//        accuracySamples = ModuleUtilLib.addSample(accuracySamples, accuracy, sampleCount);
//        //accuracyAvg = ModuleUtilLib.calcAverage(accuracySamples, sampleCount);
//

// "Time of Log,Provider,Epoch Time,Date and Time,Time to First Location Fix (milliseconds)," 
// + "CPU Usage,"
// + "Latitude,"
// + "Longitude,"
// + "Location Changed,"
// + "Accuracy,"
// + "Accuracy Changed,"
// + "No of Satellites Used,"
// + "No of Unused Satellites,"
// + "Ratio of Used and Unused Satellites,"
// + "Satellite IDs (PRNs),"
// + "Satellites Changed,"
// + "SNR,"
// + "SNR Changed,"
// + "Unused SatPRNs,"
// + "Unused SatPRNs Changed,"
// + "Unused SatSNRs,"
// + "unusedSNRChanged,"
// + "Time of Reading SNR,MinSNR,MaxSNR,SNRrange,AvgSNR,StdDevSNR";
        String stats = provider + "," +
                time + "," +
                Integer.toString(time2firstFix) + "," +
                Double.toString(dataProcessor.trustScore) + "," +
                Float.toString(cpuUsage) + "," +
                Double.toString(latitude) + "," +
                Double.toString(longitude) + "," +
                Integer.toString(dataProcessor.locationChanged? 1:0) + "," +
                Float.toString(accuracy) + "," +
                Integer.toString(dataProcessor.accuracyChanged ? 1 : 0) + "," +
				Integer.toString(noOfSatUsed) + "," +
				Integer.toString(noOfSatUnused) + "," +
				Double.toString(ratioOfUsedUnusedSat) + "," +
				satPrn + "," +
                Integer.toString(dataProcessor.satPRNsChanged? 1: 0) + "," +
                satSnr + "," +
				Integer.toString(dataProcessor.satSNRsChanged? 1 : 0) + "," +
                unusedSatPRNs + "," +
				Integer.toString(dataProcessor.unusedSatPRNsChanged? 1 : 0) + "," +
                unusedSatSNRs + "," +
                Integer.toString(dataProcessor.unusedSatSNRsChanged? 1 : 0) + "," +
                snrTimes + "," +
                minSNR + "," +
                maxSNR + "," +
                rangeSNR + "," +
                avgSNR + "," +
                stdDevSNR;
        // String stats = provider + "," +
                // Long.toString(timeEpoch) + "," +
                // time + "," +
                // Float.toString(cpuUsage) + "," +
                // Double.toString(latitude) + "," +
                // //Double.toString(latitudeAvg) + "," +
                // Double.toString(longitude) + "," +
                // //Double.toString(longitudeAvg) + "," +
                // Float.toString(accuracy) + "," +
                // //Double.toString(accuracyAvg) + "," +
                // Integer.toString(time2firstFix) + "," +
                // satPrn + "," +
                // satSnr + "," +
                // unusedSatPRNs + "," +
                // unusedSatSNRs + "," +
                // snrTimes + "," +
                // minSNR + "," +
                // maxSNR + "," +
                // rangeSNR + "," +
                // avgSNR + "," +
                // stdDevSNR;
//
        logger.log(stats, true);

        if (DEBUGGING) {
            publishModuleUpdate(stats);
            Log.d(TAG, "CPU Usage: " + Float.toString(cpuUsage) +
                    ", Provider: " + provider +
                    ", Latitude: " + Double.toString(latitude) +
                    ", Latitude Average: " + Double.toString(latitudeAvg) +
                    ", Longitude: " + Double.toString(longitude) +
                    ", Longitude Average: " + Double.toString(longitudeAvg) +
                    ", Accuracy: " + Float.toString(accuracy) +
                    ", Accuracy Average: " + Double.toString(accuracyAvg) +
                    ", Epoch Time of Fix: " + timeEpoch +
                    ", Time of Fix: " + time);
        }
    }
    //endregion

    private void init() {
        simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (locationManager == null) {
            locationManager = getLocationManager(context);

        } else {
            try {
                locationManager.removeUpdates(this);
            } catch (java.lang.SecurityException se) {
                Log.e(TAG, "Security Exception Thrown");
            } catch (java.lang.IllegalArgumentException iae) {
                Log.e(TAG, "Illegal Argument Exception Thrown");
            }
        }
        if (useMockLocations) {
            try {
                locationManager.addTestProvider(MOCK_PROVIDER_NAME, false, false, false, false, true, true, true, 0, 20);
            } catch (java.lang.IllegalArgumentException ex) {
                Log.w(TAG, "Mock Location Provider Already Exists");
            }
            locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);
        }
        setProviderInfo(getCurrentProvider(locationManager));
        if (provider != null) {
            try {
                locationManager.requestLocationUpdates(provider, TIME_BETWEEN_UPDATES, DISTANCE_CHANGE_BETWEEN_UPDATES, this);
                locationManager.addGpsStatusListener(this);
            } catch (java.lang.SecurityException ex) {
                Log.e(TAG, "Location Permission Not Enabled");
            }
        }
    }

    private synchronized void setTime2firstFix(int time2firstFix) {
        this.time2firstFix = time2firstFix;
    }

    private synchronized void setSatPrn(String satPrn) {
        this.satPrn = satPrn;
    }

    private synchronized void setSatSnr(String satSnr) {
        this.satSnr = satSnr;
    }

    private synchronized void setUnusedSatPrn(String unusedPRNs) {
        this.unusedSatPRNs = unusedPRNs;
    }

    private synchronized void setUnusedSatSnr(String unusedSNRs) {
        this.unusedSatSNRs = unusedSNRs;
    }
    private synchronized void setNoOfSatUsed(Integer noOfSatUsed) {
        this.noOfSatUsed = noOfSatUsed;
    }

    private synchronized void  setNoOfSatUnused(Integer noOfSatUnused){
        this.noOfSatUnused = noOfSatUnused;
    }
    private synchronized void setSnrTimes(String snrTimes) {
        this.snrTimes = snrTimes;
    }

//    float min = 0, max = 0, range = 0, avg = 0, stdDev = 0
    private synchronized void setSnrMin(float min) {
        this.minSNR = min;
    }

    private synchronized void setSnrMax(float max) {
        this.maxSNR = max;
    }

    private synchronized void setSnrRange(float range) {
        this.rangeSNR = range;
    }

    private synchronized void setSnrAvg(float avg) {
        this.avgSNR = avg;
    }

    private synchronized void setSnrStDev(float stDev) {
        this.stdDevSNR = stDev;
    }

    private synchronized void setLongitudeInfo(double longitude) {
        this.longitude = longitude;
    }

    private synchronized void setLatitudeInfo(double latitude) {
        this.latitude = latitude;
    }

    private synchronized void setAccuracyInfo(float accuracy) {
        this.accuracy = accuracy;
    }

    private synchronized void setTimeInfo(long time) {

        this.time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(time));
    }

    private synchronized void setTimeInfoUTC(long epochTime) {
        this.timeEpoch = epochTime;
    }

    public double getLastLongitude() {
        return longitude;
    }

    public double getLastLatitude() {
        return latitude;
    }

    public float getLastAccuracy() {
        return accuracy;
    }

    public String getLastTime() {
        return time;
    }

    public long getLastTimeUTC() {
        return timeEpoch;
    }

    public double getAverageLongitude() {
        return longitudeAvg;
    }

    public double getAverageLatitude() {
        return latitudeAvg;
    }

    public double getAverageAccuracy() {
        return accuracyAvg;
    }

    private synchronized void setProviderInfo(String provider) {
        if (useMockLocations) {
            this.provider = MOCK_PROVIDER_NAME;
        } else {
            this.provider = provider;
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {

        GpsStatus gpsStatus = null;
        String prns = "", snrs = "", unusedPRNs = "", unusedSNRs = "", snrTimes = "", timeSNRreading = "";
        int time2FstFix = 0, noOfSatUsed=0,noOfSatUnused=0;
        float usedUnusedSatRatio=0;
        Boolean satellitesChanged =false, unusedSatellitesChanged =false, unusedSNRChanged =false;
        //long timeSNRreading = 0;

        try {
            gpsStatus = locationManager.getGpsStatus(null);
        } catch (SecurityException ex) {

            Log.e(TAG, "Security Exception for GPSstatus permission");
            return;
        }

        if (gpsStatus != null) {

            time2FstFix = gpsStatus.getTimeToFirstFix(); //Returns the time (in milliseconds) required to receive the first fix since the most recent restart of the GPS engine.

            Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> sat = satellites.iterator();
            int i = 0;
//            List<Integer> prns = new ArrayList<Integer>();
//            List<Integer> snrs = new ArrayList<Integer>();


            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                if (satellite.usedInFix()) {
//                    satPrn =  satellite.getPrn();
                    timeSNRreading = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis()));
                    snrTimes += timeSNRreading + "/";
                    prns += satellite.getPrn() + "/";
                    snrs += satellite.getSnr() + "/";
            noOfSatUsed++;
                }
                else
                {
                    //timeSNRreading2 = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis()));
                    //snrTimes2 += timeSNRreading + "/";
                    unusedPRNs += satellite.getPrn() + "/";
                    unusedSNRs += satellite.getSnr() + "/";
                    noOfSatUnused++;
                }
                setTime2firstFix(time2FstFix);
                setSatPrn(prns);
                setSatSnr(snrs);
                setSnrTimes(snrTimes);
                setUnusedSatPrn(unusedPRNs);
                setUnusedSatSnr(unusedSNRs);
                setNoOfSatUsed(noOfSatUsed);
                setNoOfSatUnused(noOfSatUnused);
                //snrStat(prns+ "/" + unusedPRNs,snrs+ "/" + unusedSNRs);
            }

        }
    }

    public void snrStat(String satSNRs, String satPRNs) {

        float min = 0, max = 0, range = 0, avg = 0, stdDev = 0, sd = 0, dif = 0;
        if (satSNRs!="" && satPRNs!="") {
            String[] snrs = satSNRs.split("/");
            Arrays.sort(snrs);
            min = Float.parseFloat(snrs[0]);
            max = Float.parseFloat(snrs[snrs.length - 1]);
            range = max - min;

            for (int i = 0; i < snrs.length; i++) {
                avg += Float.valueOf(snrs[i]);
            }
            avg = avg / snrs.length;

            for (int i = 0; i < snrs.length; i++) {
                dif = avg - Float.parseFloat(snrs[i]);
                dif = (float) Math.pow(dif, 2);
                sd += dif;
            }
            stdDev = (float) Math.sqrt(sd / (snrs.length - 1));

            setSnrMin(min);
            setSnrMax(max);
            setSnrRange(range);
            setSnrAvg(avg);
            setSnrStDev(stdDev);
        }
    }

//    /**
//     * Adapted From: http://stackoverflow.com/questions/22405403/android-cpu-cores-reported-in-proc-stat
//     * @return
//     */
//    private static float getCpuUsage(int numCores){
//        float[] coreLoads = new float[numCores];
//        for(int i = 0; i < numCores; i++){ // Get load of CPU cores
//            float load = getCoreLoad(i);
//            if(!Float.isNaN(load)){ // If the CPU load gathered is a number
//                coreLoads[i] = load;
//            }
//        }
//        float usage = 0;
//        for (float coreLoad : coreLoads) {
//            usage += coreLoad;
//        }
//        return usage / coreLoads.length;
//    }
//    /**
//     * Adapted From: http://stackoverflow.com/questions/22405403/android-cpu-cores-reported-in-proc-stat
//     * @param core
//     * @return
//     */
//    private static float getCoreLoad(int core){
//        try{
//            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
//            for(int i = 0; i < (core + 1); i++){
//                reader.readLine();
//            }
//            String load = reader.readLine();
//            if(load.contains("cpu")){
//                String[] toks = load.split(" ");
//                long work1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
//                long total1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
//                        Long.parseLong(toks[4]) + Long.parseLong(toks[5])
//                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
//                try{
//                    Thread.sleep(CORE_LOAD_WAIT_TIME);
//                }catch(java.lang.InterruptedException ex){
//                    Log.w(TAG,"Core Load Wait Time Interrupted");
//                }
//                reader.seek(0);
//                for(int i = 0; i < (core + 1); i++){
//                    reader.readLine();
//                }
//                load = reader.readLine();
//                if(load.contains("cpu")){
//                    reader.close();
//                    toks = load.split(" ");
//                    long work2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
//                    long total2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
//                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
//                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
//                    return (float)(work2 - work1) / ((total2 - total1));
//                }else{
//                    reader.close();
//                    return 0;
//                }
//            }else{
//                reader.close();
//                return 0;
//            }
//        }catch(java.io.IOException ex){
//            ex.printStackTrace();
//        }
//        return 0;
//    }
}
