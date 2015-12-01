testNumber="07"
gcc resource-manager.c -std=c11 -o resource-manager
./resource-manager --verbose testing/input/input-$testNumber
cat testing/output/output-$testNumber
