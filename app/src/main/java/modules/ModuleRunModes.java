package modules;

/**
 * Relates Run Mode Names With Assigned Run Mode Integers.
 *
 * @author William Oliff
 */

public final class ModuleRunModes {
    /**
     * Allows a module to run forever and will only stop when {@code stopModule} method of the
     * module is called
     */
    public static final int CONTINUOUS_RUN_MODE = 0;
    /**
     * A module will only run for the specified number of run times
     */
    public static final int LIMITED_RUN_MODE = 1;
}
