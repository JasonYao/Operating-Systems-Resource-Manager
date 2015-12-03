import java.util.ArrayList;

/**
 * Resource object class
 */
public class Resource
{
    // Resource attributes
    private int resourceID;
    private int totalAmountOfResourceAvailable;
    private int resourcesCurrentlyAvailable;
    private ArrayList<Task> taskUsageList;

    /**
     * Resource object constructor
     * @param resourceID The ID number of the resource
     * @param totalAmountOfResourceAvailable The total amount of this resource type
     * @param resourcesCurrentlyAvailable The current amount of resources available
     * @param taskUsageList The resources that are currently using this resource
     */
    public Resource (int resourceID, int totalAmountOfResourceAvailable,
                     int resourcesCurrentlyAvailable, ArrayList<Task> taskUsageList)
    {
        setResourceID(resourceID);
        setTotalAmountOfResouceAvailable(totalAmountOfResourceAvailable);
        setResourcesCurrentlyAvaillable(resourcesCurrentlyAvailable);
        setTaskUsageList(taskUsageList);
    } // End of the Resource method

    public int getResourceID() {
        return resourceID;
    }

    public void setResourceID(int resourceID) {
        this.resourceID = resourceID;
    }

    public int getTotalAmountOfResouceAvailable() {
        return totalAmountOfResourceAvailable;
    }

    public void setTotalAmountOfResouceAvailable(int totalAmountOfResourceAvailable) {
        this.totalAmountOfResourceAvailable = totalAmountOfResourceAvailable;
    }

    public int getResourcesCurrentlyAvaillable() {
        return resourcesCurrentlyAvailable;
    }

    public void setResourcesCurrentlyAvaillable(int resourcesCurrentlyAvailable) {
        this.resourcesCurrentlyAvailable = resourcesCurrentlyAvailable;
    }

    public ArrayList<Task> getTaskUsageList() {
        return taskUsageList;
    }

    public void setTaskUsageList(ArrayList<Task> taskUsageList) {
        this.taskUsageList = taskUsageList;
    }
} // End of the resource class
