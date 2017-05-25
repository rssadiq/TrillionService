package modules;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

/**
 * @author William Oliff
 */

public class UsageModule extends Module{
    // Global Constants
    private static final int CORE_LOAD_WAIT_TIME = 200, NUM_OF_SAMPLES = 10; // Wait time between cpu core load reads and number of rolling samples to store
    private static final String MODULE_NAME = "Usage_Module", TAG = "USAGE MODULE"; // Default module name and logging tag
    private static final String LOG_FILE_HEADER = "Time of Log,CPU,CPU Avg,RAM,RAM Avg,Battery,Battery Avg"; // Log file header of data names
    private static final boolean DEBUGGING = false; // Debugging Flag
    // Rolling data sample arrays
    private float[] cpuSamples = new float[NUM_OF_SAMPLES], batSamples = new float[NUM_OF_SAMPLES];
    private double[] ramSamples = new double[NUM_OF_SAMPLES];
    // Variables to store latest data from gathered sample
    private float cpu, battery;
    private double ram, cpuAvg, ramAvg, batteryAvg;
    private int numCores;
    private boolean charging, antivirusInstalled;
    /**
     *
     * @param context
     */
    public UsageModule(Context context){ // Default Constructor
        super(context,MODULE_NAME);
    }

//    /**
//     *
//     * @param context
//     * @param runMode
//     */
//    public UsageModule(Context context, int runMode){
//        super(context,MODULE_NAME,runMode);
//    }
//    /**
//     *
//     * @param context
//     * @param runMode
//     * @param moduleName
//     */
//    public UsageModule(Context context, int runMode, String moduleName){
//        super(context,moduleName,runMode);
//    }
//    /**
//     *
//     * @param context
//     * @param runMode
//     * @param moduleName
//     * @param numOfRuns
//     * @param sleepTime
//     */
//    public UsageModule(Context context,int runMode, String moduleName, int numOfRuns, int sleepTime){
//        super(context,moduleName,runMode,numOfRuns,sleepTime);
//    }


    @Override
    protected void collectConstSample(){
        numCores = getNumCores();
        charging = isCharging(context);
        antivirusInstalled = ModuleUtilLib.isAntivirusInstalled(context);
        String stats = "Number of Cores:," + Integer.toString(numCores) +
                ",Device Charging:," + Boolean.toString(charging) +
                ",Anti-Virus Installed:," + Boolean.toString(antivirusInstalled) +
                ",Averages From:," + NUM_OF_SAMPLES + " Samples";
        logger.log(stats,false);
        logger.log(LOG_FILE_HEADER,false);
        if(DEBUGGING){
            publishModuleUpdate(LOG_FILE_HEADER);
            publishModuleUpdate(stats);
            Log.d(TAG,stats);
        }
    }
    @Override
    protected void collectSample(){
        cpu = getCpuUsage(numCores);
        cpuSamples = ModuleUtilLib.addSample(cpuSamples,cpu, sampleCount);
        cpuAvg = ModuleUtilLib.calcAverage(cpuSamples, sampleCount);
        ram = getMemoryUsed(context);
        ramSamples = ModuleUtilLib.addSample(ramSamples,ram, sampleCount);
        ramAvg = ModuleUtilLib.calcAverage(ramSamples, sampleCount);
        battery = getBatteryLevel(context);
        batSamples = ModuleUtilLib.addSample(batSamples,battery, sampleCount);
        batteryAvg = ModuleUtilLib.calcAverage(batSamples, sampleCount);
        String stats = Float.toString(cpu) + "," +
                Double.toString(cpuAvg)  + "," +
                Double.toString(ram) + "," +
                Double.toString(ramAvg) + "," +
                Float.toString(battery) + "," +
                Double.toString(batteryAvg);
        logger.log(stats,true);
        if(DEBUGGING){
            publishModuleUpdate(stats);
            Log.d(TAG,"CPU: " + Float.toString(cpu * 100) +
                    "%, CPU Average: " + Double.toString(cpuAvg * 100) +
                    "%, RAM: " + Double.toString(ram * 100) +
                    "%, RAM Average: " + Double.toString(ramAvg * 100) +
                    "%, Battery: " + Float.toString(battery * 100) +
                    "%, Battery Average: " + Double.toString(batteryAvg * 100));
        }
    }
    public int getNumOfCores(){
        return numCores;
    }
    public boolean isDeviceCharging(){
        return charging;
    }
    public float getLastCpuUsage(){
        return cpu;
    }
    public double getLastRamUsage(){
        return ram;
    }
    public float getLastBatteryLevel(){
        return battery;
    }
    public float[] getCpuSamples(){
        return cpuSamples;
    }
    public double[] getRamSamples(){
        return ramSamples;
    }
    public float[] getBatterySamples(){
        return batSamples;
    }
    public double getAverageCpu(){
        return cpuAvg;
    }
    public double getAverageRam(){
        return ramAvg;
    }
    public double getAverageBattery(){
        return batteryAvg;
    }
    /**
     * Adapted From: http://stackoverflow.com/questions/22405403/android-cpu-cores-reported-in-proc-stat
     * @return
     */
    //    private static float getCpuUsage(int numCores){
    public static float getCpuUsage(int numCores){

        float[] coreLoads = new float[numCores];
        for(int i = 0; i < numCores; i++){ // Get load of CPU cores
            float load = getCoreLoad(i);
            if(!Float.isNaN(load)){ // If the CPU load gathered is a number
                coreLoads[i] = load;
            }
        }
        float usage = 0;
        for (float coreLoad : coreLoads) {
            usage += coreLoad;
        }
        return usage / coreLoads.length;
    }
    /**
     * Adapted From: http://stackoverflow.com/questions/22405403/android-cpu-cores-reported-in-proc-stat
     * @param core
     * @return
     */
    private static float getCoreLoad(int core){
        try{
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            for(int i = 0; i < (core + 1); i++){
                reader.readLine();
            }
            String load = reader.readLine();
            if(load.contains("cpu")){
                String[] toks = load.split(" ");
                long work1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                long total1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                        Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                try{
                    Thread.sleep(CORE_LOAD_WAIT_TIME);
                }catch(java.lang.InterruptedException ex){
                    Log.w(TAG,"Core Load Wait Time Interrupted");
                }
                reader.seek(0);
                for(int i = 0; i < (core + 1); i++){
                    reader.readLine();
                }
                load = reader.readLine();
                if(load.contains("cpu")){
                    reader.close();
                    toks = load.split(" ");
                    long work2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                    long total2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                    return (float)(work2 - work1) / ((total2 - total1));
                }else{
                    reader.close();
                    return 0;
                }
            }else{
                reader.close();
                return 0;
            }
        }catch(java.io.IOException ex){
            ex.printStackTrace();
        }
        return 0;
    }
    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * From: http://stackoverflow.com/questions/7962155/how-can-you-detect-a-dual-core-cpu-on-an-android-device-from-code
     * @return The number of cores, or 1 if failed to get result
     */
//    private static int getNumCores(){
    public static int getNumCores(){
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]+", pathname.getName());
            }
        }
        try{
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        }catch (Exception e){
            return 1;
        }
    }
    /**
     * Gets the current battery level of the device the application is running on.
     * Adapted From: https://developer.android.com/training/monitoring-device-state/battery-monitoring.html
     * @param context Application / Activity context
     * @return Returns the battery level of the device as a percentage.
     */
    private static float getBatteryLevel(Context context){
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        // Get the level of the battery and return it.
        if(batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (float) level / (float) scale;
        }
/*
        long currentInstantMicroAmperes = 0;

        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        currentInstantMicroAmperes = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
*/

        return 0;
    }
    /**
     * Checks if the device is charging.
     * Adapted From: https://developer.android.com/training/monitoring-device-state/battery-monitoring.html
     * @param context Application / Activity context
     * @return Charging status of the device.
     */
    private static boolean isCharging(Context context){
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        if(batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_FULL || status == BatteryManager.BATTERY_STATUS_CHARGING;
        }
        return false;
    }
    /**
     * Gets the amount of memory used as a percentage.
     * Adapted From: http://stackoverflow.com/questions/3170691/how-to-get-current-memory-usage-in-android
     * @param context Application / Activity context
     * @return Percentage of memory used
     */
    private static double getMemoryUsed(Context context){
        MemoryInfo memoryInfo = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        double availMemMega = memoryInfo.availMem / 1048576L, totalMemMega = memoryInfo.totalMem / 1048576L;
        return availMemMega / totalMemMega;
    }
}


