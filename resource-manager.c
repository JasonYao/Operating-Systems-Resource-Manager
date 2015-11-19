#include <stdlib.h>
#include <stdio.h>
#include <string.h>

typedef enum {false, true} bool;        // Allows boolean types in C

bool IS_VERBOSE_MODE; // Flags whether the output should be detailed or not

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
    
    return EXIT_SUCCESS;
} // End of the main function