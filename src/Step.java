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
    private Step nextStep;
    private Step previousStep;
    private boolean isMarkedForRemovalFromDeadlock;
    private boolean shouldBeSkipped;

    public Step(int stepID, int stepType, int group, Task referencedTask, Object referencedResource,
                int numberOfResourcesUtilised, Step nextStep, Step previousStep, boolean isMarkedForRemovalFromDeadlock, boolean shouldBeSkipped)
    {
        setShouldBeSkipped(shouldBeSkipped);
        setMarkedForRemovalFromDeadlock(isMarkedForRemovalFromDeadlock);
        setPreviousStep(previousStep);
        setNextStep(nextStep);
        setStepID(stepID);
        setStepType(stepType);
        setGroup(group);

        setReferencedTask(referencedTask);
        setReferencedResource(referencedResource);
        setNumberOfResourcesUtilised(numberOfResourcesUtilised);
    } // End of the step object constructor

    public boolean isMarkedForRemovalFromDeadlock() {
        return isMarkedForRemovalFromDeadlock;
    }

    public void setMarkedForRemovalFromDeadlock(boolean markedForRemovalFromDeadlock) {
        isMarkedForRemovalFromDeadlock = markedForRemovalFromDeadlock;
    }

    public boolean isShouldBeSkipped() {
        return shouldBeSkipped;
    }

    public void setShouldBeSkipped(boolean shouldBeSkipped) {
        this.shouldBeSkipped = shouldBeSkipped;
    }

    public Step getPreviousStep() {
        return previousStep;
    }

    public void setPreviousStep(Step previousStep) {
        this.previousStep = previousStep;
    }

    public Step getNextStep() {
        return nextStep;
    }

    public void setNextStep(Step nextStep) {
        this.nextStep = nextStep;
    }

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
