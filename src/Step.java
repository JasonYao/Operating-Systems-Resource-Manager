/**
 * Step object class
 */
public class Step
{
    // Step attributes
    private int stepID;
    private int stepType; // 0 = initiate, 1 = compute, 2 = request, 3 = release, 4 = terminate
    private int group;

    private Task referencedTask;
    private Object referencedResource;
    private int numberOfResourcesUtilised;

    public Step(int stepID, int stepType, int group, Task referencedTask, Object referencedResource,
                int numberOfResourcesUtilised)
    {
        setStepID(stepID);
        setStepType(stepType);
        setGroup(group);

        setReferencedTask(referencedTask);
        setReferencedResource(referencedResource);
        setNumberOfResourcesUtilised(numberOfResourcesUtilised);
    } // End of the step object constructor

    public int getStepID() {
        return stepID;
    }

    public void setStepID(int stepID) {
        this.stepID = stepID;
    }

    public int getStepType() {
        return stepType;
    }

    public void setStepType(int stepType) {
        this.stepType = stepType;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public Task getReferencedTask() {
        return referencedTask;
    }

    public void setReferencedTask(Task referencedTask) {
        this.referencedTask = referencedTask;
    }

    public Object getReferencedResource() {
        return referencedResource;
    }

    public void setReferencedResource(Object referencedResource) {
        this.referencedResource = referencedResource;
    }

    public int getNumberOfResourcesUtilised() {
        return numberOfResourcesUtilised;
    }

    public void setNumberOfResourcesUtilised(int numberOfResourcesUtilised) {
        this.numberOfResourcesUtilised = numberOfResourcesUtilised;
    }
} // End of the step object class
