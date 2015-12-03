import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SimulateResourceManagers
{
    // Simulation global variables
    private static ArrayList<Resource> resourceContainer;
    private static ArrayList<Task> taskContainer;
    private static ArrayList<Step> stepContainer;
    private static long CURRENT_CYCLE_TIME;
    private static boolean IS_VERBOSE_MODE = false;
    private static int NUMBER_OF_GROUPS = 0;

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
        CURRENT_CYCLE_TIME = 0;

        // Fills each container with the respective objects
        fillAllContainers(args[validateInput(args)]);

        testAllContainers();

        // Runs the simulation, prints the output, and resets it for the next run (0 = ORM, 1 = Banker's)
        simulationWrapper(0);
        simulationWrapper(1);
    } // End of the main method

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
                taskContainer.add(new Task(i, 0, 0, 0, 0, new ArrayList<>()));

            // Removes the next line from the read
            fileScanner.nextLine();

            boolean isFirstTime = true;
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
                stepContainer.add(new Step(i, convertedStepType, NUMBER_OF_GROUPS,
                        taskContainer.get(referencedTaskRaw - 1), referencedResource, numberOfResourcesUtilised));

                isFirstTime = false;
                ++i;
                subScanner.close();
            }
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
    } // End of the fill all containers method


    private static void simulationWrapper(int version)
    {
        try
        {
            switch (version)
            {
                case 0:
                    // Opportunistic resource manager
                    break;
                case 1:
                    // Banker's algorithm resource manager
                    break;
                default:
                    throw new InvalidInputException(
                            "Error: simulation wrapper has called an invalid manager algorithm");
            }
            printOutput();
            reset();
        }
        catch (InvalidInputException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    } // End of the simulation wrapper method

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
            currentTask.setResourcesInUse(new ArrayList<>());

            currentTask.setStartTime(0);
            currentTask.setWaitTime(0);
            currentTask.setStopTime(0);
            currentTask.setStatus(0);
        }

        // Resets time
        CURRENT_CYCLE_TIME = 0;

    } // End of the reset method

    private static void printOutput()
    {
        //TODO

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

        // Tests the task container
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

        //TODO remove after testing
//        for (int i = 0; i < stepContainer.size(); ++i)
//        {
//            if ((stepContainer.get(i).getStepType() == 0) || (stepContainer.get(i).getStepType() == 1)
//                    || (stepContainer.get(i).getStepType() == 3))
//            {
//                System.out.println("The step type for initiate, request, or release is: " +
//                        stepContainer.get(i).getStepType());
//                Resource test = (Resource) stepContainer.get(i).getReferencedResource();
//                System.out.println("The referenced Resource has a resource ID of:" + test.getResourceID());
//
//                System.out.println("The referenced task has a task ID of: " +
//                        stepContainer.get(i).getReferencedTask().getTaskID());
//
//                System.out.println("The group this is a part of is: " + stepContainer.get(i).getGroup());
//            }
//        } //TODO end of testing

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
                System.out.print(" " + currentTask.getResourcesInUse().get(currentResource.getResourceID()));
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
            for (Tuple currentTuple : currentTask.getResourcesInUse())
            {
                System.out.print(" " + currentTuple.getResource().getResourceID());
                System.out.print("(" + currentTuple.getAmountOfResource() + ")");
            }
            System.out.println();
        }
    } // End of the test task container method

} // End of the simulate resource managers class
