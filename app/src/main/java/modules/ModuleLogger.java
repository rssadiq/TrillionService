package modules;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates and handles logger objects to be used by Modules to log there gathered sample data.
 *
 * @author William Oliff
 */

class ModuleLogger {
    // Private global variables
    private String filename, fileDir;
    private File root, dir, file;
    private FileOutputStream fileOutputStream;
    private PrintWriter printWriter;
    private Context context;
    private boolean logFileOpen = false;
    // Private Global Constants
    private static final String TAG = "ModLog", APP_DIR = "/Trillion", FILE_EXT = ".csv";
    private static final boolean DEBUGGING = true; // Debugging Flag

    /**
     * Constructor. Creates a new module logger object. Also, will create a directory for the passed
     * module name, if the directory doesn't already exist; and will create (if the file doesn't
     * already exist) and open a log file under the passed filename. If {@param fileTimeStamp} iS
     * set to true, the current system time and date will be appended to the created log file name.
     *
     * @param context       The context of the activity being called from.
     * @param filename      The filename of the log file to be created and/or opened.
     * @param fileTimeStamp Specifies if the current system time and date is to be appended to the
     *                      log file.
     * @param moduleName    The name of module that the module logger object is being created by.
     */
    protected ModuleLogger(Context context, String filename, boolean fileTimeStamp, String moduleName) {
        this.context = context;
        this.filename = checkForWhiteSpace(filename);
        if (fileTimeStamp) {
            this.filename += "_" + new SimpleDateFormat("dd.MM.yyyy.HH.mm.ss.SSS").format(new Date());
        }
        this.fileDir = "/" + checkForWhiteSpace(moduleName);
        initialiseLogging();
    }

    /**
     * Closes any current open log file and creates a new file in the same directory currently being
     * used under the passed filename.
     *
     * @param filename      The filename of the log file to be created and/or opened.
     * @param fileTimeStamp Specifies if the current system time and date is to be appended to the
     *                      log file.
     */
    protected void newLogFile(String filename, boolean fileTimeStamp) {
        if (logFileOpen) { // If a log file is open, close it
            closeLogFile();
        }
        this.filename = checkForWhiteSpace(filename);
        if (fileTimeStamp) {
            this.filename += "_" + new SimpleDateFormat("dd.MM.yyyy.HH.mm.ss.SSS").format(new Date());
        }
        initialiseLogging();
    }

    /**
     * Closes the current open log file. Note, this method needs to be called for the file to become
     * discoverable.
     */
    protected void closeLogFile() {
        try {
            // Close writer and stream
            printWriter.flush();
            printWriter.close();
            fileOutputStream.close();
//            // Scan for closed file to make discoverable
//            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
            logFileOpen = false;
        } catch (java.io.IOException ex) {
            Log.e(TAG, "IO Exception Thrown");
        }
        finally {
            // Scan for closed file to make discoverable
            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
        }
    }

    /**
     * Add a new entry to the current open log file.
     *
     * @param message      The message string to be logged.
     * @param addTimeStamp Specifies if the current system time and date is to be added the log
     *                     entry.
     */
    protected void log(String message, boolean addTimeStamp) {
        if (logFileOpen) {
            if (addTimeStamp) {
                printWriter.println(new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss.SSS").format(new Date()) + "," + message);
            } else {
                printWriter.println(message);
            }
        }
    }

    /**
     * Gets the current log filename.
     *
     * @return Returns the current log file name.
     */
    protected String getFilename() {
        return filename;
    }

    /**
     * Gets the current file directory being used.
     *
     * @return Returns the current file directory used.
     */
    protected String getFileDir() {
        return dir.toString();
    }

    /**
     * Gets the location path of the current log file.
     *
     * @return Returns the location path of the log file.
     */
    protected String getFileLocation() {
        return file.toString();
    }

    /**
     * Gets the {@code logFileOpen} status flag.
     *
     * @return {@code true} if a log file is currently open else, returns {@code false}
     */
    protected boolean isLogFileOpen() {
        return logFileOpen;
    }

    /**
     * Performs the required initialise to create and open a new log file
     */
    private void initialiseLogging() {
        if (!(isExternalStorageWritable())) { // Check that the external storage can be written to
            return;
        }
        root = Environment.getExternalStorageDirectory(); // Get the root directory of the device
        dir = new File(root.getAbsoluteFile() + APP_DIR + fileDir); // Add the application and file directory to the directory path
//
//        // When the parent service is Forced to Stop, everything stops immediately.
//        // So, the Log files are not closed properly and they are not vissible from
//        // the PC because the MediaScannerConnection.scanFile is not executed.
//        // We're running this command MediaScannerConnection.scanFile here so that
//        // it makes those files available when the service restarts.
//        MediaScannerConnection.scanFile(context, new String[]{dir.toString()}, null, null);

        dir.mkdirs(); // Make the directory
        file = new File(dir, filename + FILE_EXT); // Create the file the directory, with set file extension
        if (DEBUGGING) {
            Log.d(TAG, "Created File: " + file.toString());
        }
        try {
            // Setup and open stream and writer
            fileOutputStream = new FileOutputStream(file);
            printWriter = new PrintWriter(fileOutputStream);
            logFileOpen = true;
        } catch (java.io.FileNotFoundException ex) {
            Log.e(TAG, "File Not Found Exception Thrown");
        }
        finally {
            // When the parent service is Forced to Stop, everything stops immediately.
            // So, the Log files are not closed properly and they are not vissible from
            // the PC because the MediaScannerConnection.scanFile is not executed.
            // We're running this command MediaScannerConnection.scanFile here so that
            // it makes those files available when the service restarts.
            MediaScannerConnection.scanFile(context, new String[]{dir.toString()}, null, null);
        }

    }

    /**
     * Checks if external storage is available for read and write
     *
     * @return Returns true if writable else false
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        Log.e(TAG, "Can't write to external storage");
        return false;
    }

    /**
     * Checks for white/empty space in the passed {@code String} and replaces any found white/empty
     * space with an underscore.
     *
     * @param strToCheck The {@code String} to be checked.
     * @return The checked string with any white/empty space replaced with underscores
     */
    private String checkForWhiteSpace(String strToCheck) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < strToCheck.length(); i++) { // For the length of the string
            if (strToCheck.charAt(i) == ' ') { // If white space, replace with underscore
                str.append('_');
            } else {
                str.append(strToCheck.charAt(i));
            }
        }
        return str.toString();
    }
}
