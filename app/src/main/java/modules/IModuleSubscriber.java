package modules;

/**
 * Interface for Module Subscribers. Need to implement this interface to be able to subscribe to
 * module and relieve published messages from modules.
 *
 * @author William Oliff
 */

public interface IModuleSubscriber {
    /**
     * Called when a module has been started.
     * @param module The module that was started.
     */
    void moduleStarted(Module module);
    /**
     * Called when a module publishes an update. For example, if the module was interrupted during
     * its operation.
     * @param module The module publishing the update.
     * @param message The message string from the module, detailing what occurred.
     */
    void moduleUpdate(Module module, String message);
    /**
     * Called when a module has completed its operation.
     * @param module The module that has completed its operation.
     */
    void moduleComplete(Module module);
}
