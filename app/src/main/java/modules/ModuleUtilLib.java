package modules;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.BuildConfig;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Auxiliary Class. Provides general methods that can be used by every module.
 *
 * @author William Oliff
 */
final class ModuleUtilLib {
    private static final String TAG = "ModuleUtilLib"; // Logging tag
    private static final boolean DEBUGGING = false; // Debugging flag
    private static final String ANTIVIRUS_WHITE_LIST = "/storage/emulated/0/Trillion/Antivirus_White_List.txt";
    // Possible root directory paths
    // From: http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
    private static final String rootPaths[] = { "/system/app/Superuser.apk",
                                                "/sbin/su",
                                                "/system/bin/su",
                                                "/system/xbin/su",
                                                "/data/local/xbin/su",
                                                "/data/local/bin/su",
                                                "/system/sd/xbin/su",
                                                "/system/bin/failsafe/su",
                                                "/data/local/su",
                                                "/su/bin/su"};
    /**
     * Get the current Android OS build version code of the device.
     * @return Current version code of Android OS
     */
    public static int getBuildVersion(){
        return BuildConfig.VERSION_CODE;
    }
    /**
     * Get the current Android OS build version name of the device.
     * @return Current version name of Android OS
     */
    public static String getBuildVersionName(){
        return BuildConfig.VERSION_NAME;
    }
    /**
     * Adapted From: http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
     * @return
     */
    public static boolean isDeviceRooted(){
        return checkBuildTags() || checkDirectoryPaths() || checkExecutableCommands();
    }
    /**
     * Checks if the build tags contains test keys rather than release keys. If test keys is found
     * device may have root access.
     * Adapted From: http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
     * @return True, if the build tags for the device contains test-keys
     */
    public static boolean checkBuildTags(){
        String tags = Build.TAGS;
        if(tags != null && tags.toLowerCase().contains("test-keys")){
            Log.d(TAG,"Found test-keys in build tags");
            return true;
        }
        return false;
    }
    /**
     * Checks for the device having an exciting root directory path.
     * Adapted From: http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
     * @return True, if a root directory path exists
     */
    public static boolean checkDirectoryPaths(){
        for(String rootPath: rootPaths){ // Check all possible root directory path
            if(new File(rootPath).exists()){
                Log.d(TAG,"Found root directory path: " + rootPath);
                return true;
            }
        }
        return false;
    }
    /**
     * Checks for the device having the ability to execute commands it wouldn't normally have
     * permissions to do so.
     * Adapted From: http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
     * @return {@code true}, if device can execute commands it normally couldn't
     */
    public static boolean checkExecutableCommands(){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{ "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null){ // If the process has executed
                Log.d(TAG,"Able to execute root commands");
                return true;
            }else{
                return false;
            }
        } catch (java.io.IOException ex) {
            return false;
        } finally {
            if(process != null){ // If the process is still running, kill it
                process.destroy();
            }
        }
    }
    /**
     * Checks if the device allows applications to be installed from unknown sources. If, unable to
     * find setting will return true.
     * Adapted From: http://stackoverflow.com/questions/6333602/programatically-find-whether-installing-from-unknown-sources-is-allowed
     * @return {@code true}, if device is allowed to install applications from unknown sources
     */
    public static boolean unknownSourcesAllowed(Context context){
        try{
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
        }catch(Settings.SettingNotFoundException ex){
            return true;
        }
    }
    /**
     * Checks for approved anti-virus being installed on the device.
     * Adapted From: http://stackoverflow.com/questions/2695746/how-to-get-a-list-of-installed-android-applications-and-pick-one-to-run
     * @param context The context of the activity being called from
     * @return Returns {@code true} is an antivirus is found, otherwise returns false
     */
    public static boolean isAntivirusInstalled(Context context){
        boolean antivirusFound = false;
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(ANTIVIRUS_WHITE_LIST));
            String dataLine, packageName = null, processName = null;
            StringBuilder str = new StringBuilder();
            char ch;
            int colCount = 0;
            while((dataLine = in.readLine()) != null){ // For all lines in the white list
                for(ApplicationInfo packageInfo: packages){ // For all packages
                    for(int i = 0; i < dataLine.length(); i++){ // For every character in the data line read
                        ch = dataLine.charAt(i);
                        if(ch == ','){
                            if(colCount == 0){
                                packageName = str.toString();
                            }
                            colCount++;
                            str = new StringBuilder();
                        }else{
                            str.append(ch);
                        }
                    }
                    if(colCount == 1){
                        processName = str.toString();
                    }
                    str = new StringBuilder();
                    colCount = 0;
                    if(packageInfo.packageName.equals(packageName) && packageInfo.processName.equals(processName)){ // Check if installed package  matches current item in the white list
                        antivirusFound = true;
                        break;
                    }
                    packageName = null;
                    processName = null;
                }
                if(antivirusFound){
                    break;
                }
            }
        }catch (java.io.FileNotFoundException ex){
            Log.e(TAG, "Unable to find antivirus white list");
            return false;
        }catch (java.io.IOException ex){
            Log.e(TAG, "IO Exception Thrown");
            return false;
        }finally{
            if(in != null) {
                try{in.close();}catch(Exception ex){ex.printStackTrace();}
            }
        }
        if(DEBUGGING) {
            for (ApplicationInfo packageInfo : packages) {
                Log.d(TAG, "Installed package :" + packageInfo.packageName);
            }
            Log.d(TAG, "Anti-Virus Found: " + antivirusFound);
        }
        return antivirusFound;
    }
    /**
     * Calculates the average of the passed array of values, with in the range passed. If the passed
     * range is greater than the length of the array, the range will be set the length of the array.
     * @param values The array of values the average is to be calculated from.
     * @param range The range of values to used in calculating the average. Note, this is 0 indexed.
     * @return The calculated average of values in the specified range.
     */
    public static double calcAverage(float values[], int range){
        if(range >= values.length){
            range = values.length - 1;
        }
        double average = 0.0;
        int count = 0;
        for(int i = 0; i <= range; i++){
            average += values[i];
            count++;
        }
        return (average / count);
    }
    /**
     * Adds a new sample to the passed array at the specified position. If the passed position is
     * greater than the array length the new sample will be passed at the end of the array and all
     * all elements will shifted left. Hence, the element at index location 0 is lost.
     * @param samples The arrays of samples the new sample is to be added to.
     * @param newSample The new sample to be added to the array.
     * @param position The position at which the new sample is to be added.
     * @return Returns the passed samples array with the new sample added.
     */
    public static float[] addSample(float[] samples, float newSample, int position){
        if(position < samples.length){
            samples[position] = newSample;
            return samples;
        }else{
            for(int i = 0; i < (samples.length - 1); i++){
                samples[i] = samples[i + 1];
            }
            samples[(samples.length - 1)] = newSample;
        }
        return samples;
    }
    /**
     * Calculates the average of the passed array of values, with in the range passed. If the passed
     * range is greater than the length of the array, the range will be set the length of the array.
     * @param values The array of values the average is to be calculated from.
     * @param range The range of values to used in calculating the average. Note, this is 0 indexed.
     * @return The calculated average of values in the specified range.
     */
    public static double calcAverage(double values[], int range){
        if(range >= values.length){
            range = values.length - 1;
        }
        double average = 0.0;
        int count = 0;
        for(int i = 0; i <= range; i++){
            average += values[i];
            count++;
        }
        return (average / count);
    }
    /**
     * Adds a new sample to the passed array at the specified position. If the passed position is
     * greater than the array length the new sample will be passed at the end of the array and all
     * all elements will shifted left. Hence, the element at index location 0 is lost.
     * @param samples The arrays of samples the new sample is to be added to.
     * @param newSample The new sample to be added to the array.
     * @param position The position at which the new sample is to be added.
     * @return Returns the passed samples array with the new sample added.
     */
    public static double[] addSample(double[] samples, double newSample, int position){
        if(position < samples.length){
            samples[position] = newSample;
            return samples;
        }else{
            for(int i = 0; i < (samples.length - 1); i++){
                samples[i] = samples[i + 1];
            }
            samples[(samples.length - 1)] = newSample;
        }
        return samples;
    }
    /**
     * Calculates the average of the passed array of values, with in the range passed. If the passed
     * range is greater than the length of the array, the range will be set the length of the array.
     * @param values The array of values the average is to be calculated from.
     * @param range The range of values to used in calculating the average. Note, this is 0 indexed.
     * @return The calculated average of values in the specified range.
     */
    public static double calcAverage(long values[], int range){
        if(range >= values.length){
            range = values.length - 1;
        }
        double average = 0.0;
        int count = 0;
        for(int i = 0; i <= range; i++){
            average += values[i];
            count++;
        }
        return (average / count);
    }
    /**
     * Adds a new sample to the passed array at the specified position. If the passed position is
     * greater than the array length the new sample will be passed at the end of the array and all
     * all elements will shifted left. Hence, the element at index location 0 is lost.
     * @param samples The arrays of samples the new sample is to be added to.
     * @param newSample The new sample to be added to the array.
     * @param position The position at which the new sample is to be added.
     * @return Returns the passed samples array with the new sample added.
     */
    public static long[] addSample(long[] samples, long newSample, int position){
        if(position < samples.length){
            samples[position] = newSample;
            return samples;
        }else{
            for(int i = 0; i < (samples.length - 1); i++){
                samples[i] = samples[i + 1];
            }
            samples[(samples.length - 1)] = newSample;
        }
        return samples;
    }
}
