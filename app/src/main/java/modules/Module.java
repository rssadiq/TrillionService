package modules;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates, handles and provides implementation for module objects.
 *
 * @author William Oliff
 */
public abstract class Module {
    // run mode, number of runs (samples), sleep between samples, current number of samples collected
    protected int mode = 0, runs = 5, sleep = 1000, sampleCount = 0;
    protected boolean running = false; // running status flag
    protected Context context; // context of the activity that created module
    protected String name = "Module"; // module name
    protected List<IModuleSubscriber> subscribers = new ArrayList<>(); // module subscribers
    protected ModuleLogger logger; // logger object for module
    //****
    protected DataProcessor dataProcessor; // processor object for module

    private ModuleTask moduleTask = null; // The task of the module
    private Thread moduleThread = null; // The module thread to execute module task
    /**
     * Default Constructor. Creates a module for the passed context and with given name.
     * @param context The context in which the module is a part of
     * @param moduleName The name to be assigned to the module
     */
    protected Module(Context context, String moduleName){ // Default Constructor
        this.context = context;
        name = moduleName;
        sampleCount = 0;
        moduleTask = new ModuleTask();
    }

    //region UNNECESSARY CONSTRUCTORS
//    /**
//     * Constructor. Creates a module for the passed context and with given name and run mode.
//     * @param context The context in which the module is a part of
//     * @param moduleName The name to be assigned to the module
//     * @param runMode The run mode of the mode
//     */
//    protected Module(Context context, String moduleName, int runMode){
//        this(context,moduleName);
//        mode = runMode;
//    }
//    /**
//     * Constructor. Creates a module for the passed context and with given name, run mode, number of
//     * runs (samples to collect) and the sleep time (ms) between runs/samples.
//     * @param context The context in which the module is a part of
//     * @param moduleName The name to be assigned to the module
//     * @param runMode The run mode of the mode
//     * @param numOfRuns
//     * @param sleepTime
//     */
//    protected Module(Context context, String moduleName, int runMode, int numOfRuns, int sleepTime){
//        this(context,moduleName);
//        mode = runMode;
//        runs = numOfRuns;
//        sleep = sleepTime;
//    }
    //endregion


    /**
     * Called each time the module is to collect a sample
     */
    protected abstract void collectSample();
    /**
     * Called when a module is started on a new run, allowing for constant / static data to be
     * collected
     */
    protected abstract void collectConstSample();
    /**
     * Starts the module. If the module is already running, will wait for that task to finish
     */
    public void startModule(){
        try {
            if(moduleThread != null && moduleThread.isAlive()) { // If the thread for the module isn't null and alive
                moduleThread.join(); // Wait for the thread to finish
            }
            moduleThread = new Thread(moduleTask); // Start new module task and start
            moduleThread.start();
            sampleCount = 0; // Reset sampleCount count
        }catch (java.lang.InterruptedException ex){
            ex.printStackTrace();
        }
    }
    /**
     * Stops the module, if the module is currently running.
     */
    public void stopModule(){
        if(moduleThread != null && moduleThread.isAlive()) { // If the thread for the module isn't null and alive
            moduleThread.interrupt(); // Interrupt the thread
        }
        sampleCount = 0; // Reset sampleCount count
    }
    /**
     * Checks is the module is currently running or not.
     * @return Returns the running status flag of the module.
     */
    public boolean isRunning(){
        return running;
    }
    /**
     * Sets the run mode of the module. Check the "ModuleRunModes" for the available run modes.
     * @param newRunMode The new run mode of the module.
     */
    public void setRunMode(int newRunMode){
        mode = newRunMode;
    }
    /**
     * Gets the run mode of the module.
     * @return Returns the current run mode of the module.
     */
    public int getRunMode(){
        return mode;
    }
    /**
     * Sets the number of runs/samples of the module.
     * @param newNumOfRuns The new number of runs of the module.
     */
    public void setNumOfRuns(int newNumOfRuns){
        runs = newNumOfRuns;
    }
    /**
     * Gets the num of runs set to the module.
     * @return Returns the current number of runs of the module.
     */
    public int getNumOfRuns(){
        return runs;
    }
    /**
     * Sets the sleep time between runs of the module.
     * @param newSleepTime The new sleep time of the module.
     */
    public void setSleepTime(int newSleepTime){
        sleep = newSleepTime;
    }
    /**
     * Gets the sleep time between runs of the module.
     * @return Returns the current sleep time of the module.
     */
    public int getSleepTime() {
        return sleep;
    }
    /**
     * Gets the current sample count of the module. The number of samples the module as collect
     * during its current run. Returns 0 if the module is not running.
     * @return Returns the current sample count of the module.
     */
    public int getSampleCount(){
        return sampleCount;
    }
    /**
     * Sets the name assigned to the module. Note, if the name matches the name of another module
     * the may cause effect the logging process.
     * @param newName The new module name to be assign to the module.
     */
    public void setModuleName(String newName){
        name = newName;
    }
    /**
     * Gets the assigned name of the module.
     * @return Returns the current name of the module.
     */
    public String getModuleName(){
        return name;
    }
    /**
     * Sets the context to which the module belongs to.
     * @param newContext The new context to be set.
     */
    public void setContext(Context newContext){
        context = newContext;
    }
    /**
     * Gets the context which the module currently belongs to.
     * @return Returns the current context of the module.
     */
    public Context getContext(){
        return context;
    }
    /**
     * Adds the passed subscriber to the module list of subscribers.
     * @param subscriber The new subscriber to be added.
     */
    public void addSubscriber(IModuleSubscriber subscriber) {
        if(!subscribers.contains(subscriber)){ // If the subscriber is not in the list, add it
            subscribers.add(subscriber);
        }
    }
    /**
     * Removes the passed subscriber from the module list of subscribers, if the subscriber exists
     * in the list.
     * @param subscriber The subscriber to be removed.
     */
    public void removeSubscriber(IModuleSubscriber subscriber) {
        if(subscribers.contains(subscriber)){ // If the subscriber is in the list, remove it
            subscribers.remove(subscriber);
        }
    }
    /**
     * Called when the module has been started and alerts all current subscribers.
     */
    protected void publishModuleStarted(){
        for(IModuleSubscriber subscriber: subscribers){
            subscriber.moduleStarted(this);
        }
    }
    /**
     * Called when the module has finished its task and alerts all current subscribers.
     */
    protected void publishModuleComplete() {
        for(IModuleSubscriber subscriber : subscribers){
            subscriber.moduleComplete(this);
        }
    }
    /**
     * Called or to be invoked when the module state changes and/or information needs to be sent to
     * all current subscribers.
     * @param message The message string to be send to subscribers.
     */
    protected void publishModuleUpdate(String message){
        for(IModuleSubscriber subscriber : subscribers){
            subscriber.moduleUpdate(this,message);
        }
    }
    /**
     * The task to be performed by the module thread
     */
    private class ModuleTask implements Runnable{
        @Override
        public void run(){
            try{
                running = true;
                publishModuleStarted(); // Publish module has started

                dataProcessor = new DataProcessor(context); // Create a new DataProcessor object

                logger = new ModuleLogger(context,name,true,name); // Create a new logger object and log file
                collectConstSample(); // Collect constant data
                if(mode == ModuleRunModes.CONTINUOUS_RUN_MODE){ // If the mode of the module is continuous
                    while(mode == ModuleRunModes.CONTINUOUS_RUN_MODE){ // While module mode is continuous, loop forever
                        collectSample(); // Collect variable sample information
                        sampleCount++;
                        Thread.sleep(sleep);
                    }
                }else if(mode == ModuleRunModes.LIMITED_RUN_MODE){ // If the mode of the module is limited
                    for(int i = 0; i < getNumOfRuns(); i++){ // Run the assigned number of times and then break
                        collectSample(); // Collect variable sample information
                        sampleCount++;
                        Thread.sleep(sleep);
                    }
                }
                publishModuleComplete(); // Publish module has completed
            }catch(java.lang.InterruptedException ex){
                publishModuleUpdate(name + " Was Interrupted"); // Publish module has been interrupted
            } finally {
                System.out.println("***Closing Log File***");
                logger.closeLogFile(); // Close the current log file
                running = false;
            }
        }
    }

}

