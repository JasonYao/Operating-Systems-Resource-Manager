#include <stdlib.h>
#include <stdio.h>
#include <string.h>

typedef enum {false, true} bool;                        // Allows boolean types in C

struct Task {
    uint32_t taskID;                                // Identity of the task (ordered by appearance)
    uint32_t startTime;                             // The cycle that this task initiates a request
    uint32_t terminateTime;                         // The cycle that this task terminates
    uint32_t timeSpentWaiting;                      // The amount of time spent waiting for a resource to free up
    uint8_t status;                                 // 0 = not started, 1 = computing, 2 = blocked,
                                                    // 3 = terminated, 4 = aborted
    bool hasAlreadyBeenCorrectedInDeadlock;
};

// Constants & flags
bool IS_VERBOSE_MODE;                                   // Flags whether the output should be detailed or not
const uint32_t SUPER_LARGE_ACTIVITY_BUFFER = 300;       // Maximum number of activity lines in one instance
const uint32_t MAXIMUM_NUMBER_OF_TASKS = 100;           // Maximum allowed tasks in one instance
const uint8_t MAXIMUM_GROUP_LENGTH = 50;                // [ARBITRARY] Set max number of groups to 50

// Semi-constants (constants, but can only be instantiated at runtime)
uint32_t TOTAL_CREATED_TASKS = 0;                       // The total number of tasks created at read-in
uint32_t TOTAL_CREATED_RESOURCES = 0;                   // The total number of resources created at read-in
uint32_t TOTAL_CREATED_ACTIVITIES = 0;                  // The total number of activities added to the activities list
uint32_t TOTAL_ACTIVTY_GROUPS = 0;                      // The total number of activity groups (actual)
uint32_t GREATEST_GROUP_LENGTH = 0;                     // The maximum out of all group lengths
uint32_t GROUP_LENGTHS[MAXIMUM_GROUP_LENGTH];           // Array holding the length of each group

// Programme variables
//uint32_t CURRENT_TIME_CYCLE = 0;                        // The current cycle time in a run
uint32_t NUMBER_OF_TERMINATED_TASKS = 0;

// Aborted tasks array stuff
uint32_t ABORTED_TASK_ARRAY[MAXIMUM_NUMBER_OF_TASKS];   // An array holding the task ID of the aborted task
uint32_t numberOfAbortedTasks = 0;                      // The number of aborted tasks

// Deadlocked tasks array stuff
struct Task deadlockedTasks[MAXIMUM_NUMBER_OF_TASKS];       // The array of tasks that have been marked as deadlocked
uint32_t numberOfDeadlockedTasks = 0;                       // The number of tasks that are considered deadlocked

/* Defines a resource struct */
struct Resource {
    uint32_t resourceID;                                        // Identity of the resource (ordered by load)
    uint32_t currentResourcesAvailable;                         // Number of this resource currently available
    uint32_t totalResourceCount;                                // The total number of resources this type has
    uint32_t taskArrayUtilisation[MAXIMUM_NUMBER_OF_TASKS];     // An array showing which tasks have which resources
    // NOTE: Inefficient as fuck, but very simplistic to implement
};

struct Activity {
    uint32_t activityID;
    uint8_t typeOfActivity;                 // 0 = initiate, 1 = compute, 2 = request, 3 = release, 4 = terminate
    uint32_t group;                         // The group that this activity is a part of

    uint32_t taskNumber;                    // The task that this activity is referencing
    uint32_t resourceType;                  // The resource type this activity is referencing
    uint32_t numberOfResourcesRequested;    // The number of resources that are initially_claimed, requested or released
};




/************************* Start of helper functions *************************/

/**
 * Resets all activities between simulation runs
 */
void reset(struct Resource resourceContainer[], struct Task taskContainer[])
{
    // Resets the time
    CURRENT_TIME_CYCLE = 0;

    // Resets the tasks
    for (uint32_t i = 0; i < TOTAL_CREATED_TASKS; ++i)
    {
        taskContainer[i].startTime = 0;
        taskContainer[i].status = 0;
        taskContainer[i].terminateTime = 0;
        taskContainer[i].timeSpentWaiting = 0;
    }

    // Resets the resources
    for (uint32_t i = 0; i < TOTAL_CREATED_RESOURCES; ++i)
    {
        resourceContainer[i].currentResourcesAvailable = resourceContainer[i].totalResourceCount;
        // Zeros the resource array (faster & safer than memset)
        for (uint32_t j = 0; j < TOTAL_CREATED_TASKS; ++j)
            resourceContainer[i].taskArrayUtilisation[j] = 0;
    }

} // End of the reset function

/**
 * Outputs the result of the current run
 */
void printRun(struct Resource resourceContainer[], struct Task taskContainer[], uint8_t currentRun)
{
    switch (currentRun)
    {
        case 0:
            printf("\t\tFIFO\n");
            break;
        case 1:
            printf("\t\tBANKER'S\n");
            break;
        default:
            fprintf(stderr, "Error: Invalid run number given during print out!\n");
            exit(1);
    } // End of printint out statements for each run

    uint32_t totalWaitTime = 0;
    // Iterates through the tasks and prints out the per task metadata
    for (uint32_t i = 0; i < TOTAL_CREATED_TASKS; ++i)
    {
        if (taskContainer[i].status == 4)
        {
            // Task was aborted early, prints abort message
            printf("\tTask %i  \taborted\n", taskContainer[i].taskID + 1);
        }
        else
        {
            // Amount of time taken to complete the task (includes blocked time)
            uint32_t turnAround = taskContainer[i].terminateTime - taskContainer[i].startTime;

            totalWaitTime += taskContainer[i].timeSpentWaiting;

            // Percentage of time spent waiting
            double percentageOfTimeSpentWaiting = ((double) taskContainer[i].timeSpentWaiting/
                                                   ((double) turnAround))*(100.000000);

            printf("\tTask %i  \t%i\t%i\t%6f%%\n", taskContainer[i].taskID + 1, turnAround,
                   taskContainer[i].timeSpentWaiting, percentageOfTimeSpentWaiting);
        }
    }

    // Calculates the total percentage of time spent waiting
    double globalPercentageOfTimeSpentWaiting = ((double)totalWaitTime/((double)CURRENT_TIME_CYCLE
                                                                        - NUMBER_OF_TERMINATED_TASKS))*(100.000000);
    uint32_t timeRun;
    if ((TOTAL_ACTIVTY_GROUPS == TOTAL_CREATED_TASKS) ||
            ((TOTAL_ACTIVTY_GROUPS - numberOfAbortedTasks) == TOTAL_CREATED_TASKS))
        timeRun = CURRENT_TIME_CYCLE - NUMBER_OF_TERMINATED_TASKS;
    else
    {
        printf("Total activity groups is: %i\n", TOTAL_ACTIVTY_GROUPS);
        uint32_t indexOfLongestRunningTask = 0;
        for (uint32_t i = 0; i < TOTAL_CREATED_TASKS; ++i)
        {
            if ((taskContainer[indexOfLongestRunningTask].terminateTime >  taskContainer[i].terminateTime)
                && (taskContainer[i].status == 3))
            {indexOfLongestRunningTask = i;}
        } // End of searching for the highest time
        timeRun = taskContainer[indexOfLongestRunningTask].terminateTime;
    }

    // Prints out the global totals
    printf("\tTotal\t\t%i\t%i\t%6f%%\n", timeRun, totalWaitTime,
           globalPercentageOfTimeSpentWaiting);

} // End of the print run function

void testShit(struct Activity activityList[], struct Resource resourceContainer[], struct Task taskContainer[])
{
    //Activity
    for (uint32_t i = 0; i < TOTAL_CREATED_ACTIVITIES; ++i)
    {
        printf("Activity: ID: %i\n", activityList[i].activityID);
        printf("Activity: Type: %i\n", activityList[i].typeOfActivity);
        printf("Activity: Resource Type: %i\n", activityList[i].resourceType);
        printf("Activity: Task Number: %i\n", activityList[i].taskNumber);
        printf("Activity: Number of Resources Requested: %i\n", activityList[i].numberOfResourcesRequested);
    }

    // Resource
    for (uint32_t i = 0; i < TOTAL_CREATED_RESOURCES; ++i)
    {
        printf("Resource: Resource ID: %i\n", resourceContainer[i].resourceID);
        printf("Resource: Current resources available: %i\n", resourceContainer[i].currentResourcesAvailable);
        printf("Resource: Total resource count: %i\n", resourceContainer[i].totalResourceCount);
        for (uint32_t j = 0; j < TOTAL_CREATED_TASKS; ++j)
            printf("Resource: Task Array Utilisation: %i\n", resourceContainer[i].taskArrayUtilisation[j]);
    }

    // Task
    for (uint32_t i = 0; i < TOTAL_CREATED_TASKS; ++i)
    {
        printf("Task: ID: %i\n", taskContainer[i].taskID);
        printf("Task: Status: %i\n", taskContainer[i].status);
        printf("Task: Start Time: %i\n", taskContainer[i].startTime);
        printf("Task: End Time: %i\n", taskContainer[i].terminateTime);
        printf("Task: Time spent waiting: %i\n", taskContainer[i].timeSpentWaiting);
    }
} // End of the testing suite

void testTask(struct Task taskContainer[])
{
    // Task
    for (uint32_t i = 0; i < TOTAL_CREATED_TASKS; ++i)
    {
        printf("Task: ID: %i\n", taskContainer[i].taskID);
        printf("Task: Status: %i\n", taskContainer[i].status);
        printf("Task: Start Time: %i\n", taskContainer[i].startTime);
        printf("Task: End Time: %i\n", taskContainer[i].terminateTime);
        printf("Task: Time spent waiting: %i\n", taskContainer[i].timeSpentWaiting);
    }
} // End of the test Task testing suite

/************************* End of helper functions *************************/

/************************* Start of flag setters *************************/

/**
 * Sets global flags for output depending on user input
 * @param argc The number of arguments in argv, where each argument is space deliminated
 * @param argv The command used to run the program, with each argument space deliminated
 */
uint8_t setFlags(int32_t argc, char *argv[])
{
    if (argc == 2)
        return 1;
    else if (argc == 3)
    {
        if (strcmp(argv[1], "--verbose") == 0)
        {
            IS_VERBOSE_MODE = true;
            return 2;
        }
        else
        {
            fprintf(stderr, "Error: Invalid commandline argument formatting!\n");
            exit(1);
        }
    }
    else
    {
        fprintf(stderr, "Error: Invalid commandline argument formatting!\n");
        exit(1);
    }
} // End of the setFlags function

/**
 * Given a string, sets the activity type to the correct one.
 * 0 = initiate, 1 = compute, 2 = request, 3 = release, 4 = terminate
 * @param type The activity type in digit form
 */
uint8_t setActivityType(char* type)
{
    if (strcmp(type, "initiate") == 0)
        return 0;
    else if (strcmp(type, "compute") == 0)
        return 1;
    else if (strcmp(type, "request") == 0)
        return 2;
    else if (strcmp(type, "release") == 0)
        return 3;
    else if (strcmp(type, "terminate") == 0)
        return 4;
    else
    {
        fprintf(stderr, "Error: Activity list has an invalid type: %s\n", type);
        exit(1);
    }
} // End of the set activity type function

/************************* End of flag setters *************************/

// Task statuses: 0 = not started, 1 = computing, 2 = blocked, 3 = terminated, 4 = aborted

void opportunisticResourceControlFixed(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                                     struct Resource resourceContainer[], struct Task taskContainer[], uint32_t row,
                                     uint32_t column)
{
    // Makes call once, caching in local variables for faster access
    uint8_t currentType = activityList[row][column].typeOfActivity;
    uint32_t taskIndex = activityList[row][column].taskNumber - 1;
    uint32_t currentResourceType = activityList[row][column].resourceType;
    uint32_t currentNumberOfResourcesDealtWith = activityList[row][column].numberOfResourcesRequested;
    uint32_t resourceIndex = currentResourceType - 1;

    // Sanity check: checks whether a task is already terminated, throws an error if so
    if (taskContainer[taskIndex].status == 3)
    {
        fprintf(stderr, "Error: Attempting to make a task do an action after termination!\n");
        exit(1);
    }
    else if (taskContainer[taskIndex].status == 4)
    {return;}





} // End of dealing with opportunistic resource control (fixed)



/************************* Start of simulation functions *************************/
void opportunisticResourceControl(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                                   struct Resource resourceContainer[], struct Task taskContainer[], uint32_t row,
                                   uint32_t column)
{
    // Makes call once, caching in local variables for faster access
    uint8_t currentType = activityList[row][column].typeOfActivity;
    uint32_t taskIndex = activityList[row][column].taskNumber - 1;
    uint32_t currentResourceType = activityList[row][column].resourceType;
    uint32_t currentNumberOfResourcesDealtWith = activityList[row][column].numberOfResourcesRequested;
    uint32_t resourceIndex = currentResourceType - 1;

    // Sanity check: checks whether a task is already terminated, throws an error if so
    if (taskContainer[taskIndex].status == 3)
    {
        fprintf(stderr, "Error: Attempting to make a task do an action after termination!\n");
        exit(1);
    }
    else if (taskContainer[taskIndex].status == 4)
        return;

    // Start of the actual opportunistic manager
    if (currentType == 0)
    {
        // Activity is initial claim for resources from a task (ignored on optimistic)
        taskContainer[taskIndex].startTime = CURRENT_TIME_CYCLE;
        if (IS_VERBOSE_MODE)
        {
            printf("%i: For task number: %i, number of resource %i initially claimed was: %i, ignored, start time "
                           "is at: %i\n", activityList[row][column].activityID, taskIndex + 1, resourceIndex + 1,
                   currentNumberOfResourcesDealtWith, taskContainer[taskIndex].startTime);
        }
    }
    else if (currentType == 2)
    {
        // Activity is normal request for resources
        // Allocates resources if available
        if (resourceContainer[resourceIndex].currentResourcesAvailable >= currentNumberOfResourcesDealtWith)
        {
            resourceContainer[resourceIndex].currentResourcesAvailable -= currentNumberOfResourcesDealtWith;
            resourceContainer[resourceIndex].taskArrayUtilisation[taskIndex] += currentNumberOfResourcesDealtWith;
            taskContainer[taskIndex].status = 1;
            if (IS_VERBOSE_MODE)
            {
                printf("%i: For task number: %i, number of resource %i requested was: %i, success - current "
                               "cycle time is: %i\n", activityList[row][column].activityID, taskIndex + 1,
                       resourceIndex + 1, currentNumberOfResourcesDealtWith, CURRENT_TIME_CYCLE);
            }
        } // End of dealing when resources are available
        else
        {
            // Manager was not able to allocate resources to this, causes the task to block
            taskContainer[taskIndex].status = 2;
            taskContainer[taskIndex].timeSpentWaiting += 1;

            // Updates the list of deadlocked tasks TODO check if correct
            deadlockedTasks[numberOfDeadlockedTasks] = taskContainer[taskIndex];
            ++numberOfDeadlockedTasks;

            if (IS_VERBOSE_MODE)
            {
                printf("%i: For task number: %i, number of resource %i requested was: %i, failed - "
                               "current cycle time is: %i\n",
                       activityList[row][column].activityID, taskIndex + 1, resourceIndex + 1,
                       currentNumberOfResourcesDealtWith, CURRENT_TIME_CYCLE);
            }
        } // End of dealing when resources are not available
    } // End of dealing with initial claims/requests
    else if (currentType == 1)
    {
        // Task is computing (will make no more requests/release for a certain number of cycles)
        // Adds the number of cycles the task is busy with computation: only works for given datasets
        CURRENT_TIME_CYCLE += currentResourceType;
        if (IS_VERBOSE_MODE)
        {
            printf("%i: For task number: %i, it is computing at this step\n",
                   activityList[row][column].activityID, taskIndex + 1);
        }
    } // End of dealing with computing
    else if (currentType == 3)
    {
        // Activity is releasing some resources that are now un-needed by the task
        resourceContainer[resourceIndex].currentResourcesAvailable += currentNumberOfResourcesDealtWith;
        resourceContainer[resourceIndex].taskArrayUtilisation[taskIndex] -= currentNumberOfResourcesDealtWith;
        if (IS_VERBOSE_MODE)
        {
            printf("%i: For task number: %i, number of resource %i released was: %i\n",
                   activityList[row][column].activityID, taskIndex + 1, resourceIndex + 1,
                   currentNumberOfResourcesDealtWith);
        }
    } // End of dealing with resource releasing
    else if (currentType == 4)
    {
        // Activity is terminating a task
        taskContainer[taskIndex].terminateTime = CURRENT_TIME_CYCLE;

        ++NUMBER_OF_TERMINATED_TASKS;

        // Releases all resources utilised by this task
        for (uint32_t i = 0; i < TOTAL_CREATED_RESOURCES; ++i)
        {
            resourceContainer[i].currentResourcesAvailable += resourceContainer[i].taskArrayUtilisation[taskIndex];
            resourceContainer[i].taskArrayUtilisation[taskIndex] = 0;
        }
        // Updates the status
        taskContainer[taskIndex].status = 3;

        if (IS_VERBOSE_MODE)
        {
            printf("%i: For task number: %i, activity was terminated on: %i\n",
                   activityList[row][column].activityID, taskIndex + 1, taskContainer[taskIndex].terminateTime);
        }
    } // End of dealing with terminated tasks
    else
    {
        fprintf(stderr, "Error: Invalid activity type selected!\n");
        exit(1);
    }
} // End of opportunistic resource control function

/**
 * A deadlock is defined as when all non-terminated or aborted tasks have outstanding requests that
 * the manager cannot satisfy.
 */
bool isDeadlocked()
{
    uint32_t numberOfAliveTasks = TOTAL_CREATED_TASKS - (numberOfAbortedTasks + NUMBER_OF_TERMINATED_TASKS);
    if ((numberOfAliveTasks == numberOfDeadlockedTasks) && (numberOfAliveTasks != 0))
    {
        // Deadlock was found
        return true;
    }
    else
    {
        // Deadlock was not found
        return false;
    }
} // End of the isDeadlock function

void dealWithDeadlock(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                      struct Resource resourceContainer[], struct Task taskContainer[], uint32_t row)
{
// Dead lock was found for the given instruction time

    // Finds the lowest-currently running task
    uint32_t lowestNumberedDeadlockedTaskIndex = 0;
    for (uint32_t i = 0; i < numberOfDeadlockedTasks; ++i)
    {
        if (deadlockedTasks[i].taskID < deadlockedTasks[lowestNumberedDeadlockedTaskIndex].taskID)
            lowestNumberedDeadlockedTaskIndex = i;
    } // End of finding the lowest-numbered deadlocked task

    // Aborts the lowest running task
    uint32_t taskIndex = taskContainer[deadlockedTasks[lowestNumberedDeadlockedTaskIndex].taskID].taskID;
    taskContainer[deadlockedTasks[lowestNumberedDeadlockedTaskIndex].taskID].status = 4;
    taskContainer[deadlockedTasks[lowestNumberedDeadlockedTaskIndex].taskID].terminateTime = CURRENT_TIME_CYCLE;

    ABORTED_TASK_ARRAY[numberOfAbortedTasks] = taskIndex;
    ++numberOfAbortedTasks;
    if (IS_VERBOSE_MODE)
    {
        printf("For task number: %i, activity was aborted on: %i\n", taskIndex + 1,
               taskContainer[taskIndex].terminateTime);
    }

    // Resets the deadlocked arrays
    numberOfDeadlockedTasks = 0;

    //TODO check whether I'll need to memset the deadlocked array for this to work or not

    // Iterates through all resources and frees them for use
    for (uint32_t i = 0; i < TOTAL_CREATED_RESOURCES; ++i)
    {
        resourceContainer[i].currentResourcesAvailable += resourceContainer[i].taskArrayUtilisation[taskIndex];
        resourceContainer[i].taskArrayUtilisation[taskIndex] = 0;
    } // End of freeing all resources tied to this task

    // Retries all entries for this current instruction time (ignore removed task)
    --row;
} // End of dealing with deadlock


void simulateOpportunisticResourceManager(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                                          struct Resource resourceContainer[], struct Task taskContainer[])
{
    for (uint32_t row = 0; row < GREATEST_GROUP_LENGTH; ++row)
    {
        for (uint32_t column = 0; column < TOTAL_ACTIVTY_GROUPS; ++column)
        {
            // Checks if this group has already finished yet
            if (row >= GROUP_LENGTHS[column])
                continue;
            opportunisticResourceControl(activityList, resourceContainer, taskContainer, row, column);
        } // End of iterating through the columns of the activity list

        bool hasAlreadyBeenDeadlocked = false;
        while (isDeadlocked() == true)
        {
            hasAlreadyBeenDeadlocked = true;
            dealWithDeadlock(activityList, resourceContainer, taskContainer, row);
            ++CURRENT_TIME_CYCLE;

            // Reruns to check whether deadlock is found again
            for (uint32_t column = 0; column < TOTAL_ACTIVTY_GROUPS; ++column)
            {
                // Checks if this group has already finished yet
                if (row >= GROUP_LENGTHS[column])
                    continue;
                opportunisticResourceControl(activityList, resourceContainer, taskContainer, row, column);
            }
            if (isDeadlocked() == true)
            {
                --CURRENT_TIME_CYCLE;
                // Fixes the waiting time for the previously deadlocked tasks
                for (uint32_t j = 0; j < TOTAL_CREATED_TASKS; ++j)
                {
                    if (taskContainer[j].hasAlreadyBeenCorrectedInDeadlock == false)
                    {
                        taskContainer[j].hasAlreadyBeenCorrectedInDeadlock = true;
                        taskContainer[j].timeSpentWaiting -= 1;
                    }
                } // End of fixing the waiting time for previously deadlocked tasks
            }
        } // End of dealing with the deadlock
            ++CURRENT_TIME_CYCLE;
    } // End of iterating through the rows of the activity list
} // End of the simulate opportunistic resource manager function

void simulateBankersAlgorithm(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                              struct Resource resourceContainer[], struct Task taskContainer[])
{

} // End of the simulate banker's algorithm function
/************************* End of simulation functions *************************/

/**
 * Wrapper function containing the setup & calls to each simulation call
 */
void simulateResourceManagementWrapper(struct Activity activityList[][SUPER_LARGE_ACTIVITY_BUFFER],
                                       struct Resource resourceContainer[], struct Task taskContainer[],
                                       uint8_t resourceManagerAlgorithm)
{
    // Switches between opportunistic resource manager to Dijkstra's Banker manager
    switch (resourceManagerAlgorithm)
    {
        case 0:
            // Opportunistic resource manager
            simulateOpportunisticResourceManager(activityList, resourceContainer, taskContainer);
            break;
        case 1:
            // Banker manager
            simulateBankersAlgorithm(activityList, resourceContainer, taskContainer);
            break;
        default:
            fprintf(stderr, "Error: Invalid resource management algorithm selected!\n");
            exit(1);
    } // End of switching between simulations

    printRun(resourceContainer, taskContainer, resourceManagerAlgorithm);
    reset(resourceContainer, taskContainer);
} // End of simulate resource managementWrapper

/**
 * Main function dealing with file inputs, validation, error handling, and simulation calling
 */
int main(int argc, char *argv[])
{
    // Reads in from file
    FILE* inputFile;
    char* filePath;

    filePath = argv[setFlags(argc, argv)]; // Sets any global flags from input

    inputFile = fopen(filePath, "r");

    // [ERROR CHECKING]: INVALID FILENAME
    if (inputFile == NULL) {
        fprintf(stderr, "Error: cannot open input file %s!\n", filePath);
        exit(1);
    }

    // Reads in T: the number of tasks to be run
    uint32_t numberOfTasks;
    fscanf(inputFile, "%i", &numberOfTasks);

    // Reads in R: The number of resource types available
    uint32_t numberOfResourceTypes;
    fscanf(inputFile, "%i", &numberOfResourceTypes);

    // Creates task, resource and activity containers
    struct Task taskContainer[numberOfTasks];
    struct Resource resourceContainer[numberOfResourceTypes];
    struct Activity activityList[SUPER_LARGE_ACTIVITY_BUFFER][SUPER_LARGE_ACTIVITY_BUFFER];

    // Populates the task container
    for (uint32_t i = 0; i < numberOfTasks; ++i)
    {
        // Iterates through the input, and only creates if it has initiated a request
        taskContainer[i].taskID = i;        // I'll just +1 to the taskID at the end when printing stuff
        taskContainer[i].status = 0;
        taskContainer[i].timeSpentWaiting = 0;
        taskContainer[i].startTime = 0;
        taskContainer[i].terminateTime = 0;
        taskContainer[i].hasAlreadyBeenCorrectedInDeadlock = false;
        ++TOTAL_CREATED_TASKS;

        //TODO remove after [tested for 2, try for 3]
//        printf("The task's ID is: %i\n", taskContainer[i].taskID);
//        printf("The task's status is: %i\n", taskContainer[i].status);
//        printf("The task's start time is: %i\n", taskContainer[i].startTime);
//        printf("The task's terminate time is: %i\n", taskContainer[i].terminateTime);
    } // End of creating the task container

    // Populates the resource container
    for (uint32_t i = 0; i < numberOfResourceTypes; ++i)
    {
        uint32_t numberOfThisResourceType;
        fscanf(inputFile, "%i", &numberOfThisResourceType);

        resourceContainer[i].resourceID = i;
        resourceContainer[i].currentResourcesAvailable = numberOfThisResourceType;
        resourceContainer[i].totalResourceCount = numberOfThisResourceType;

        // Dangerous, but other way does not work
        memset(resourceContainer[i].taskArrayUtilisation, 0, MAXIMUM_NUMBER_OF_TASKS * sizeof(int));
        //resourceContainer[i].taskArrayUtilisation = {0}; // Initialises all values to 0
        ++TOTAL_CREATED_RESOURCES;

        //TODO remove after [test for 1 done, try for 2]
//        printf("Resource ID is: %i\n", resourceContainer[i].resourceID);
//        printf("Current resources available is: %i\n", resourceContainer[i].currentResourcesAvailable);
//        printf("Total resource is: %i\n", resourceContainer[i].totalResourceCount);
    }// End of creating the resource container

    // Populates the activity list
    ssize_t bytes_read;
    size_t nBytes = 100;
    char *line;
    line = (char *) malloc (nBytes + 1);
    bool isStartOfFile = true;
    uint32_t currentActivityLine = 0;
    uint32_t currentNumberOfActivitiesInAGroup = 0;

    while ((bytes_read = getline(&line, &nBytes, inputFile)) != -1)
    {
        if (isStartOfFile == true)
            isStartOfFile = false;
        else if (bytes_read == 1)
        {
            // Line separation was read in, resets stuff per each group
            GROUP_LENGTHS[TOTAL_ACTIVTY_GROUPS] = currentNumberOfActivitiesInAGroup;
            currentNumberOfActivitiesInAGroup = 0;
            ++TOTAL_ACTIVTY_GROUPS;
        }
        else
        {
            char readInTypeBuffer[10]; // 10 is used here since there are 9 chars in `terminate`, +1 for \0
            uint32_t readInTaskNumber;
            uint32_t readInResourceType;
            uint32_t readInNumberOfResourcesRequested;

            // re-scans line read in
            sscanf(line, "%s %i %i %i", readInTypeBuffer, &readInTaskNumber, &readInResourceType, &readInNumberOfResourcesRequested);

            //TODO remove after
//            printf("ReadinTypeBuffer is: %s\n", readInTypeBuffer);
//            printf("ReadInTaskNumber is: %i\n", readInTaskNumber);
//            printf("ReadInResourceType is: %i\n", readInResourceType);
//            printf("ReadInNumberOfResourcesRequested is: %i\n", readInNumberOfResourcesRequested);

            // Actual read-in line value, reads activity line in & assigns values
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].activityID = currentActivityLine;
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].group = TOTAL_ACTIVTY_GROUPS;
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].typeOfActivity =
                    setActivityType(readInTypeBuffer);
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].taskNumber = readInTaskNumber;
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].resourceType = readInResourceType;
            activityList[currentNumberOfActivitiesInAGroup][TOTAL_ACTIVTY_GROUPS].numberOfResourcesRequested =
                    readInNumberOfResourcesRequested;

            //TODO remove after: old activity list
//            activityList[currentActivityLine].activityID = currentActivityLine;
//            activityList[currentActivityLine].group = TOTAL_ACTIVTY_GROUPS;
//            activityList[currentActivityLine].typeOfActivity = setActivityType(readInTypeBuffer);
//            activityList[currentActivityLine].taskNumber = readInTaskNumber;
//            activityList[currentActivityLine].resourceType = readInResourceType;
//            activityList[currentActivityLine].numberOfResourcesRequested = readInNumberOfResourcesRequested;

            // TODO remove after
//        printf("activityList activityID:\t\t\t%i\n", activityList[currentActivityLine].activityID);
//        printf("activityList activityType:\t\t\t%i\n", activityList[currentActivityLine].typeOfActivity);
//        printf("activityList task number:\t\t\t%i\n", activityList[currentActivityLine].taskNumber);
//        printf("activityList resource type:\t\t\t%i\n", activityList[currentActivityLine].resourceType);
//        printf("activityList number of resources requested:\t%i\n", activityList[currentActivityLine].numberOfResourcesRequested);

            ++currentActivityLine;
            ++TOTAL_CREATED_ACTIVITIES;
            ++currentNumberOfActivitiesInAGroup;
        }
    }
    // Adds the last group length
    GROUP_LENGTHS[TOTAL_ACTIVTY_GROUPS] = currentNumberOfActivitiesInAGroup;
    ++TOTAL_ACTIVTY_GROUPS;

    // Frees stuff and error handling
    free(line);
    if (ferror(inputFile))
        perror("Error: Could not read line from input file\n");

    // Finds the largest group length (largest number of rows in a column)
    for (uint32_t i = 0; i < TOTAL_ACTIVTY_GROUPS; ++i)
    {
        if (GREATEST_GROUP_LENGTH < GROUP_LENGTHS[i])
            GREATEST_GROUP_LENGTH = GROUP_LENGTHS[i];
    }

//    //TODO remove after testing: Checks group lengths
//    for (uint32_t i = 0; i < TOTAL_ACTIVTY_GROUPS; ++i)
//        printf("Number of activities in group %i is: %i\n", i, GROUP_LENGTHS[i]);

    // Activity list, resource container and task container are completed, begins simulation
    // Begins opportunistic resource management simulation
    simulateResourceManagementWrapper(activityList, resourceContainer, taskContainer, 0);

    printf("The number of aborted tasks was: %i\n", numberOfAbortedTasks);
    printf("The number of deadlocked tasks was: %i\n", numberOfDeadlockedTasks);
    // Begins Banker's Algorithm simulation
//    simulateResourceManagementWrapper(activityList, resourceContainer, taskContainer, 1);

    return EXIT_SUCCESS;
} // End of the main function