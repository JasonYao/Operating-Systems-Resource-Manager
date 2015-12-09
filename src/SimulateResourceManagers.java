import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SimulateResourceManagers
{
    // Simulation global variables
    private static ArrayList<Resource> resourceContainer;
    private static ArrayList<Task> taskContainer;
    private static ArrayList<Step> stepContainer;
    private static int CURRENT_CYCLE_TIME;
    private static boolean IS_VERBOSE_MODE;
    private static int NUMBER_OF_GROUPS = 0;
    private static boolean IS_SEQUENTIAL_TASK;
    private static boolean DEADLOCK_WAS_DETECTED;
    private static boolean MULTIPLE_DEADLOCKS_WERE_DETECTED;

    private static ArrayList<Step> currentStepContainer;

    // Ended tasks containers
    private static ArrayList<Task> terminatedTasksContainer;
    private static ArrayList<Task> abortedTasksContainer;
    private static Queue<Task> deadlockedTasksContainer;


    /**
     * Runs the simulation for both the Opportunistic Resource Manager (ORM), and the Banker's Algorithm Resource
     * Manager (Banker's)
     * @param args The commandline to run this program of the form javac
     */
    public static void main(String[] args)
    {
        // Container creation & global variable instantiations
        resourceContainer = new ArrayList<>();
        taskContainer = new ArrayList<>();
        stepContainer = new ArrayList<>();
        deadlockedTasksContainer = new LinkedList<>();
        terminatedTasksContainer = new ArrayList<>();
        currentStepContainer = new ArrayList<>();
        abortedTasksContainer = new ArrayList<>();

        CURRENT_CYCLE_TIME = 0;
        IS_VERBOSE_MODE = false;
        IS_SEQUENTIAL_TASK = true;
        DEADLOCK_WAS_DETECTED = false;
        MULTIPLE_DEADLOCKS_WERE_DETECTED = false;

        // Fills each container with the respective objects
        fillAllContainers(args[validateInput(args)]);

        //testAllContainers(); // TODO remove after

        // Runs the simulation, prints the output, and resets it for the next run (0 = ORM, 1 = Banker's)
        simulationWrapper(0);
        simulationWrapper(1);
    } // End of the main method

    /***** Application Methods *****/
    /**
     *  [Application Method] Wrapper method to call all simulation rounds
     * @param version 0 is opportunistic resource manager, 1 is for banker's algorithm
     */
    private static void simulationWrapper(int version)
    {
        try
        {
            switch (version)
            {
                case 0:
                    // Opportunistic resource manager
                    simulateORM();
                    break;
                case 1:
                    // Banker's algorithm resource manager
                    simulateBankers();
                    break;
                default:
                    throw new InvalidInputException(
                            "Error: simulation wrapper has called an invalid manager algorithm");
            }
            printOutput(version);
            reset();
        }
        catch (InvalidInputException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    } // End of the simulation wrapper method

    /**
     * [Application Method] Simulates the opportunistic resource manager
     */
    private static void simulateORM()
    {
        while (terminatedTasksContainer.size() + abortedTasksContainer.size() != taskContainer.size())
        {
            simulateACycle(false);

            while (isDeadlocked())
            {
                dealWithDeadlock();
                testForNewDeadlock();
            }

            if (DEADLOCK_WAS_DETECTED)
            {
                DEADLOCK_WAS_DETECTED = false;

                // Resets the steps back to their prior state
                for (int i = 0; i < currentStepContainer.size(); ++i)
                {
                    Step currentStep = currentStepContainer.get(i);
                    currentStepContainer.set(i, currentStep.getPreviousStep());
                }
            }
            ++CURRENT_CYCLE_TIME;
        }
    } // End of the simulate ORM method

    private static void simulateACycle(boolean isTestCycle)
    {
        for (int i = 0; i < currentStepContainer.size(); ++i)
        {
            Step currentStep = currentStepContainer.get(i);
            if (currentStep == null)
                System.out.println("Shit is still null"); //TODO remove this shit after
            sequentialTaskORM(currentStep, isTestCycle);
            currentStepContainer.set(i, currentStep.getNextStep());
        }
    } // End of the simulate a cycle method

    private static void testForNewDeadlock()
    {
        // Resets the steps back to their prior state
        for (int i = 0; i < currentStepContainer.size(); ++i)
        {
            Step currentStep = currentStepContainer.get(i);
            currentStepContainer.set(i, currentStep.getPreviousStep());
        }

        simulateACycle(true);

    } // End of the test for new deadlock method

    private static void releaseAllTaskResources(Task task)
    {
        for (Map.Entry<Resource, Integer> entry : task.getResourcesInUse().entrySet())
        {
            Resource resource = entry.getKey();
            int amountToBeReleased = entry.getValue();

            resource.setResourcesCurrentlyAvaillable(resource.getResourcesCurrentlyAvaillable() + amountToBeReleased);
            resource.getTaskUsageList().remove(task);
        }
    } // End of the release task resources method

    private static void abortTask(Task taskToBeAborted)
    {
        taskToBeAborted.setStatus(4);
        taskToBeAborted.setStopTime(CURRENT_CYCLE_TIME);
        abortedTasksContainer.add(taskToBeAborted);

        releaseAllTaskResources(taskToBeAborted);
    } // End of the abort task method

    private static void dealWithDeadlock()
    {
        // Finds the lowest currently running task
        int lowestTaskIndex = 0;
        Task lowestTask = taskContainer.get(lowestTaskIndex);
        for (Task currentTask : taskContainer)
        {
            while (taskContainer.get(lowestTaskIndex).getStatus() == 4)
            {
                // Task was aborted, increments and moves up each task
                ++lowestTaskIndex;
                lowestTask = taskContainer.get(lowestTaskIndex);
            }

            if ((currentTask.getStatus() == 1) && (lowestTask.getTaskID() > currentTask.getTaskID()))
                lowestTask = currentTask;
        }

        // Aborts the lowest currently running task
        abortTask(lowestTask);

        // Iterates through and decrements the wait time of any still alive tasks
        for (Task currentTask : taskContainer)
        {
            if ((currentTask.getStatus() != 4) && (currentTask.getStatus() != 3))
                currentTask.setWaitTime(currentTask.getWaitTime() - 1);
        }

        // Clears the deadlocked arraylist
        while (!deadlockedTasksContainer.isEmpty())
            deadlockedTasksContainer.remove();

        if (IS_VERBOSE_MODE)
        {
            int outputTask = lowestTask.getTaskID() + 1;
            System.out.println("Task #" + outputTask + ": has been aborted at time: " + CURRENT_CYCLE_TIME);
        }
    } // End of the deal with deadlock method

    /**
     * A deadlock is defined as when all non-terminated or aborted tasks have outstanding requests that
     * the manager cannot satisfy.
     */
    private static boolean isDeadlocked()
    {
        int numberOfAliveTasks = taskContainer.size() - (abortedTasksContainer.size() + terminatedTasksContainer.size());
        if ((numberOfAliveTasks == deadlockedTasksContainer.size()) && (numberOfAliveTasks != 0))
        {
            // Deadlock was found
            if (DEADLOCK_WAS_DETECTED)
                MULTIPLE_DEADLOCKS_WERE_DETECTED = true;
            DEADLOCK_WAS_DETECTED = true;
            return true;
        }
        else
        {
            // Deadlock was not found
            return false;
        }
    } // End of the isDeadlocked method

    /**
     * Method to deal with sequential tasks executed one after another inside of a single group
     */
    private static void sequentialTaskORM(Step currentStep, boolean isTestCycle)
    {
        int currentStepType = currentStep.getStepType();
        Task task = currentStep.getReferencedTask();
        int outputTaskNumber = task.getTaskID() + 1;
        int amountRequested = currentStep.getNumberOfResourcesUtilised();

        // Sanity check: checks whether a task is already terminated, throws an error if so
        if (task.getStatus() == 3)
        {
            System.err.println("Error: Attempting to make a task do an action after termination!");
            System.exit(1);
        }
        else if (task.getStatus() == 4)
        {return;}

        switch (currentStepType)
        {
            case 0:
            {
                Resource resource =  (Resource) currentStep.getReferencedResource();
                // Action is: initiate
                if (task.getStatus() == 0)
                {
                    // First time this is initiated, sets the start time
                    task.setStartTime(CURRENT_CYCLE_TIME);
                }
                task.setStatus(1);

                //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                if (IS_VERBOSE_MODE)
                {
                    System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                            " initially claims " + currentStep.getNumberOfResourcesUtilised() + " of resource "
                            + resource.getResourceID() + " at time: " + CURRENT_CYCLE_TIME);
                }
                break;
            } // End of case 0: initiate
            case 1:
            {
                // Action is: request

                Resource resource =  (Resource) currentStep.getReferencedResource();
                if (amountRequested <= resource.getResourcesCurrentlyAvaillable())
                {
                    // Enough resources are available to grant the request
                    if (isTestCycle)
                    {
                        task.setWaitTime(task.getWaitTime() + 1);
                    }
                    else
                    {
                        resource.setResourcesCurrentlyAvaillable(
                                resource.getResourcesCurrentlyAvaillable() - amountRequested);

                        // Checks whether the resource is already been requested before
                        HashMap<Resource, Integer> map = task.getResourcesInUse();
                        if (map.containsKey(resource))
                        {
                            // Resource has been requested before, adds the amount requested to the old value
                            int newValue = map.get(resource) + amountRequested;
                            map.replace(resource, newValue);
                        }
                        else
                        {
                            // Resource has not been requested before, adds a new key/ value pair
                            map.put(resource, amountRequested);
                            resource.getTaskUsageList().add(task);
                        }

                        //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                        if (IS_VERBOSE_MODE)
                        {

                            System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                                    " successfully requested " + amountRequested + " of resource "
                                    + resource.getResourceID() + " at time: " + CURRENT_CYCLE_TIME);
                        }
                    } // End of non test mode request success
                } // End of dealing with successful requests
                else
                {
                    // Not enough resources are available to grant the request
                    task.setStatus(2);
                    task.setWaitTime(task.getWaitTime() + 1);
                    deadlockedTasksContainer.add(task);

                    //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                    if (IS_VERBOSE_MODE)
                    {
                        System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                                " unsuccessfully requested " + amountRequested + " of resource "
                                + resource.getResourceID() + " at time: " + CURRENT_CYCLE_TIME);
                    }
                }
                break;

            } // End of case 1: request
            case 2:
                // Action is: compute
                // Task is computing (will make no more requests/release for a certain number of cycles)
                // Adds the number of cycles the task is busy with computation: only works for given datasets
                int computeTime = (int) currentStep.getReferencedResource();
                CURRENT_CYCLE_TIME += computeTime;

                //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                if (IS_VERBOSE_MODE)
                {
                    System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                            " is computing for " + computeTime + " at time: " + CURRENT_CYCLE_TIME);
                }
                break;
            case 3:
            {
                // Action is: release
                Resource resource =  (Resource) currentStep.getReferencedResource();
                resource.setResourcesCurrentlyAvaillable(
                        resource.getResourcesCurrentlyAvaillable() + amountRequested);
                HashMap<Resource, Integer> map = task.getResourcesInUse();
                int currentAmountUsedBeforeRelease = task.getResourcesInUse().get(resource);

                if (currentAmountUsedBeforeRelease - amountRequested == 0)
                {
                    // No more of that resource type remaining for the task
                    map.remove(resource);
                    resource.getTaskUsageList().remove(task);
                }
                else if (currentAmountUsedBeforeRelease - amountRequested > 0)
                {
                    // There is some amount of that resource type remaining for the task
                    map.replace(resource, map.get(resource) - amountRequested);
                }
                else
                {
                    // An error occurred, more resources were released than were allocated to the task
                    System.err.println("Error: More resources were released than were allocated to the task");
                    System.exit(1);
                }

                //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                if (IS_VERBOSE_MODE)
                {
                    System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                            " released " + amountRequested + " of resource "
                            + resource.getResourceID() + " at time: " + CURRENT_CYCLE_TIME);
                }
                break;
            } // End of case 3: release
            case 4:
                // Action is: terminate

                task.setStopTime(CURRENT_CYCLE_TIME);

                HashMap<Resource, Integer> map = task.getResourcesInUse();
                // Releases all resources utilised by this task
                for (Map.Entry<Resource, Integer> entry : map.entrySet())
                {
                    Resource currentResource = entry.getKey();
                    int amountToBeReleased = entry.getValue();

                    // Deals with resource stuff
                    currentResource.getTaskUsageList().remove(task);
                    int newValue = currentResource.getResourcesCurrentlyAvaillable() + amountToBeReleased;
                    currentResource.setResourcesCurrentlyAvaillable(newValue);

                    // Deals with task stuff
                    map.remove(currentResource);
                }

                // Updates the status
                task.setStatus(3);
                terminatedTasksContainer.add(task);

                //if ((IS_VERBOSE_MODE) && (!isTestCycle))
                if (IS_VERBOSE_MODE)
                {
                    System.out.println("For step #" + currentStep.getStepID() + ": Task " + outputTaskNumber +
                            " terminated at time: " + CURRENT_CYCLE_TIME);
                }
                break;
            default:
                // Action is none of the above
                System.err.println("ERROR: Invalid action was requested in ORM");
                break;
        }
    } // End of the sequential task, single group ORM method

    private static void multiTaskORM(Step currentStep)
    {

    } // End of the multitask, single group orm method

    private static void multiGroupORM()
    {
        // Single task per group

    } // End of the multi group ORM method

    /**
     * [Application Method] Simulates the banker's algorithm resource manager
     */
    private static void simulateBankers()
    {


    } // End of the simulate banker's method

    /***** Helper Methods *****/

    /**
     * [Helper Method] Helps validate the input for this program
     * @param args The commandline input given
     * @return returns the index of the filepath
     */
    private static int validateInput(String[] args)
    {
        String invalidForm = "Error: Input is invalid, please make sure the input is of the form " +
                "'java out/production/Lab3/SimulateResourceManagers <filepath>'";

        // Identifies filepath index
        int filePathIndex;
        if (args.length == 1)
            filePathIndex = 0;
        else
            filePathIndex = 1;
        try
        {
            // Checks for input numbers
            if ((args.length != 1) && (args.length != 2))
                throw new InvalidInputException(invalidForm);

            // Checks for valid verbose flag
            if (args.length != 1)
            {
                if (!args[0].equals("--verbose"))
                    throw new InvalidInputException(invalidForm);
                else
                    IS_VERBOSE_MODE = true;
            }

            // Checks for file existence
            File testFile = new File(args[filePathIndex]);
            if (!testFile.isFile())
                throw new SecurityException("Error: The input was not a valid file, please check your command input");
        }
        catch (InvalidInputException | SecurityException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return filePathIndex;
    } // End of the validate input method

    /**
     * [Helper Method] Fills all containers with created objects, given a valid filepath
     * @param filePath The input file path, given as a String
     */
    private static void fillAllContainers(String filePath)
    {
        FileReader fr = null;
        Scanner fileScanner = null;
        try
        {
            fr = new FileReader(filePath);
            fileScanner = new Scanner(fr);

            int numberOfTasks = fileScanner.nextInt(); // Reads in T: the number of tasks to be run
            int numberOfResourceTypes = fileScanner.nextInt(); // Reads in R: The number of resource types available

            // Fills the resource container
            for (int i = 0; i < numberOfResourceTypes; ++i)
            {
                int totalResourceAvailable = fileScanner.nextInt();
                if (fileScanner.hasNext())
                    resourceContainer.add(new Resource(i, totalResourceAvailable,
                            totalResourceAvailable, new ArrayList<>()));
            }

            // Fills the task container
            for (int i = 0; i < numberOfTasks; ++i)
                taskContainer.add(new Task(i, 0, 0, 0, 0, new HashMap<>()));

            // Removes the next line from the read
            fileScanner.nextLine();

            boolean isFirstTime = true;
            boolean isTask1Running = false;
            boolean isTask2Running = false;
            // Fills the step container
            int i = 0;
            while (fileScanner.hasNextLine())
            {
                String currentLine = fileScanner.nextLine();
                //System.out.println(currentLine);

                // Check to deal with input 2 (malformed input, but oh well)
                if ((isFirstTime) && (currentLine.equals("")))
                    currentLine = fileScanner.nextLine();

                // Creates the step objects
                if (currentLine.equals(""))
                {
                    ++NUMBER_OF_GROUPS;
                    continue;
                }
                Scanner subScanner = new Scanner(currentLine);
                String stepTypeRaw = subScanner.next();

                int referencedTaskRaw = subScanner.nextInt();

                // Shit to make the multigroup check work
                if ((referencedTaskRaw == 1) && (stepTypeRaw.equals("initiate")))
                    isTask1Running = true;
                else if ((referencedTaskRaw == 1) && (stepTypeRaw.equals("terminate")))
                    isTask1Running = false;
                else if ((referencedTaskRaw == 2) && (stepTypeRaw.equals("initiate")))
                    isTask2Running = true;
                else if ((referencedTaskRaw == 2) && (stepTypeRaw.equals("terminate")))
                    isTask2Running = false;

                if (IS_SEQUENTIAL_TASK)
                    checkForMultiGroup(isTask1Running, isTask2Running);
                int referencedResourceRaw = subScanner.nextInt();
                int numberOfResourcesUtilised = subScanner.nextInt();

                int convertedStepType;
                Object referencedResource;
                switch (stepTypeRaw)
                {
                    case "initiate":
                        convertedStepType = 0;
                        //referencedResource = referencedResourceRaw - 1;
                        referencedResource = resourceContainer.get(referencedResourceRaw - 1);
                        break;
                    case "request":
                        convertedStepType = 1;
                        //referencedResource = referencedResourceRaw - 1;
                        referencedResource = resourceContainer.get(referencedResourceRaw - 1);
                        break;
                    case "compute":
                        convertedStepType = 2;
                        referencedResource = referencedResourceRaw;
                        break;
                    case "release":
                        convertedStepType = 3;
                        //referencedResource = referencedResourceRaw - 1;
                        referencedResource = resourceContainer.get(referencedResourceRaw - 1);
                        break;
                    case "terminate":
                        convertedStepType = 4;
                        referencedResource = referencedResourceRaw;
                        break;
                    default:
                        throw new InvalidInputException(
                                "Error: Invalid activity found, please check the spelling in the input file");
                }
                Step stepToBeAdded = new Step(i, convertedStepType, NUMBER_OF_GROUPS,
                        taskContainer.get(referencedTaskRaw - 1), referencedResource, numberOfResourcesUtilised, null, null);
                stepContainer.add(stepToBeAdded);

                // Adds the new step if it's an initiate request and the task referenced is unique
                if (stepTypeRaw.equals("initiate"))
                {
                    boolean taskPointerIsAlreadySet = false;
                    for (Step currentStep : currentStepContainer)
                    {
                        if (currentStep.getReferencedTask().equals(stepToBeAdded.getReferencedTask()))
                            taskPointerIsAlreadySet = true;
                    }

                    if (!taskPointerIsAlreadySet)
                        currentStepContainer.add(stepToBeAdded);
                } // End of adding step if it's a unique initiate request

                isFirstTime = false;
                ++i;
                subScanner.close();
            } // End of scanning through the input file
            if (NUMBER_OF_GROUPS == 0)
                NUMBER_OF_GROUPS = 1;
        }
        catch (FileNotFoundException | InvalidInputException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        finally
        {
            if (fr != null)
            {
                try {
                    fr.close();
                } catch (IOException e)
                {
                    System.err.println("Error: Could not close input file");
                    System.exit(1);
                }
            }
            if (fileScanner != null)
                fileScanner.close();
        } // End of the finally block

        setNextAndPreviousSteps();
    } // End of the fill all containers method

    private static void setNextAndPreviousSteps()
    {
        for (int i = 0; i < stepContainer.size() - 1; ++i)
        {
            Step currentStep = stepContainer.get(i);
            int nextStepCounter = i + 1;
            Step nextStep = stepContainer.get(nextStepCounter);

            while (!nextStep.getReferencedTask().equals(currentStep.getReferencedTask()))
            {
                ++nextStepCounter;
                if (nextStepCounter >= stepContainer.size())
                {
                    nextStep = null;
                    break;
                }
                else
                    nextStep = stepContainer.get(nextStepCounter);
            }
            currentStep.setNextStep(nextStep);
            if (currentStep.getNextStep() != null)
                currentStep.getNextStep().setPreviousStep(currentStep);
        }
    } // End of the set next steps method


    private static void checkForMultiGroup(boolean isTask1Running, boolean isTask2Running)
    {
        IS_SEQUENTIAL_TASK = !((isTask1Running) && (isTask2Running));
    } // End of the check for multi group method

    /**
     * [Helper Method] Resets everything between simulation runs
     */
    private static void reset()
    {
        // Resets all resources
        for (Resource currentResource : resourceContainer)
        {
            currentResource.setTaskUsageList(new ArrayList<>());
            currentResource.setResourcesCurrentlyAvaillable(currentResource.getTotalAmountOfResouceAvailable());
        }

        // Resets all tasks
        for (Task currentTask : taskContainer)
        {
            currentTask.setResourcesInUse(new HashMap<>());
            currentTask.setStartTime(0);
            currentTask.setWaitTime(0);
            currentTask.setStopTime(0);
            currentTask.setStatus(0);
        }

        // Resets time
        CURRENT_CYCLE_TIME = 0;

    } // End of the reset method

    /**
     * [Helper Method] Prints the output of the simulation round
     */
    private static void printOutput(int version)
    {
        switch (version)
        {
            case 0:
                System.out.println("\t\tFIFO");
                break;
            case 1:
                System.out.println("\t\tBANKER'S\n");
                break;
            default:
                System.err.println("Error: Invalid run number given during print out!");
                System.exit(1);
        }

        int totalWaitTime = 0;
        // Iterates through the tasks and prints out the per task metadata
        long globalTimeRun = 0;
        for (Task currentTask : taskContainer)
        {
            // Task was aborted early, prints abort message
            if (currentTask.getStatus() == 4)
            {
                int externalPrintID = currentTask.getTaskID() + 1;
                System.out.println("\tTask " + externalPrintID + "  \taborted");
            }
            else
            {
                // Amount of time taken to complete the task (includes blocked time)
                int turnAround = currentTask.getStopTime() - currentTask.getStartTime();
                globalTimeRun += turnAround;
                totalWaitTime += currentTask.getWaitTime();

                // Percentage of time spent waiting
                double percentageOfTimeSpentWaiting = ((double) currentTask.getWaitTime() /
                        ((double) turnAround))*(100.000000);
                System.out.printf("\tTask %d  \t%d\t%d\t%6f%%\n", currentTask.getTaskID() + 1,
                        turnAround, currentTask.getWaitTime(), percentageOfTimeSpentWaiting);
            }
        }

        // Calculates the total percentage of time spent waiting
        double globalPercentageOfTimeSpentWaiting = ((double)totalWaitTime/((double) globalTimeRun))*(100.000000);

        // Prints out the global totals
        System.out.printf("\tTotal\t\t%d\t%d\t%6f%%\n", globalTimeRun, totalWaitTime,
                globalPercentageOfTimeSpentWaiting);
    } // End of the print output method

    /***** Testing Suite *****/
    /**
     * [Test Method] Tests each container to see if they have the correct internals
     */
    private static void testAllContainers()
    {
        testStepContainer();
        testResourceContainer();
        testTaskContainer();
    } // End of the test all containers method

    /**
     * [Test Method] Tests the step container for correct internals
     */
    private static void testStepContainer()
    {
        for (Step currentStep : stepContainer)
        {
            System.out.println("The current step's ID is: " + currentStep.getStepID());
            System.out.println("The current step's group is: " + currentStep.getGroup());
            System.out.println("The current step's referenced task ID is: "
                    + currentStep.getReferencedTask().getTaskID());

            if ((currentStep.getStepType() == 0) || (currentStep.getStepType() == 1)
                    || (currentStep.getStepType() == 3))
            {
                Resource currentReferencedResource = (Resource) currentStep.getReferencedResource();
                System.out.println("The current step's referenced resource ID is: "
                        + currentReferencedResource.getResourceID());

            }
            System.out.println("The current step's number of resources utilised is: "
                    + currentStep.getNumberOfResourcesUtilised());
            System.out.print("The current step's type is: ");
            try
            {
                switch (currentStep.getStepType())
                {
                    case 0:
                        System.out.println("initiate");
                        break;
                    case 1:
                        System.out.println("request");
                        break;
                    case 2:
                        System.out.println("compute");
                        break;
                    case 3:
                        System.out.println("release");
                        break;
                    case 4:
                        System.out.println("terminate");
                        break;
                    default:
                        throw new InvalidInputException(
                                "Error: Invalid activity found, please check the spelling in the input file");
                }
            }
            catch (InvalidInputException e)
            {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } // End of iterating through all steps
    } // End of the test step container method

    /**
     * [Test Method] Tests the resource container for correct internals
     */
    private static void testResourceContainer()
    {
        for (Resource currentResource : resourceContainer)
        {
            System.out.println("The current resource's ID is: " + currentResource.getResourceID());
            System.out.println("The current resource's total available amount is: "
                    + currentResource.getTotalAmountOfResouceAvailable());
            System.out.println("The current resource's current available amount is: "
                    + currentResource.getResourcesCurrentlyAvaillable());

            System.out.print("The current resource is currently in use by:");
            for (Task currentTask : currentResource.getTaskUsageList())
                System.out.print(" Task " + currentTask.getTaskID() + " (" +
                        currentTask.getResourcesInUse().get(currentResource) + ")");
            System.out.println();
        }
    } // End of the test resource container method

    /**
     * [Test Method] Tests the task container for correct internals
     */
    private static void testTaskContainer()
    {
        for (Task currentTask : taskContainer)
        {
            System.out.println("The current task's ID is: " + currentTask.getTaskID());
            System.out.println("The current task's start time is: " + currentTask.getStartTime());
            System.out.println("The current task's wait time is: " + currentTask.getWaitTime());
            System.out.println("The current task's stop time is: " + currentTask.getStopTime());
            System.out.println("The current task's status is: " + currentTask.getStatus());

            System.out.print("The current task's is using resources:");
            for (Map.Entry<Resource, Integer> entry : currentTask.getResourcesInUse().entrySet())
            {
                Resource resource = entry.getKey();
                Integer amount = entry.getValue();

                System.out.print(" " + resource.getResourceID());
                System.out.print("(" + amount + ")");
            }
            System.out.println();
        }
    } // End of the test task container method

} // End of the simulate resource managers class
