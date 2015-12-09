import java.util.HashMap;

/**
 * Task object class
 */
public class Task
{
    // Task attributes
    private int taskID;
    private int status; // 0 = not started, 1 = running, 2 = blocked, 3 = terminated, 4 = aborted
    private int startTime;
    private int stopTime;
    private int waitTime;
    private HashMap<Resource, Integer> resourcesInUse;

    /**
     * Task object constructor
     * @param taskID Identifying number for this task
     * @param status The current status of this task
     * @param startTime The start time for this task
     * @param stopTime The stop time for this task
     * @param waitTime The wait time for this task
     * @param resourcesInUse The resources in use by this task currently
     */
    public Task (int taskID, int status, int startTime, int stopTime, int waitTime,
                 HashMap<Resource, Integer> resourcesInUse)
    {
        setTaskID(taskID);
        setStatus(status);
        setStartTime(startTime);
        setStopTime(stopTime);
        setWaitTime(waitTime);
        setResourcesInUse(resourcesInUse);
    } // End of the task object constructor

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getStopTime() {
        return stopTime;
    }

    public void setStopTime(int stopTime) {
        this.stopTime = stopTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public HashMap<Resource, Integer>  getResourcesInUse() {
        return resourcesInUse;
    }

    public void setResourcesInUse(HashMap<Resource, Integer> resourcesInUse) {
        this.resourcesInUse = resourcesInUse;
    }
} // End of the task class
