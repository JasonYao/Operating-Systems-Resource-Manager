#!/bin/bash -e

testNumber="05"

# Checks whether or not the output file is created, creates one if not
if [ ! -d output ]; then
	mkdir output
	echo "Creating output directory"
fi

javac src/*.java -d output
java -cp output SimulateResourceManagers --verbose testing/input/input-$testNumber
cat testing/output/output-$testNumber
