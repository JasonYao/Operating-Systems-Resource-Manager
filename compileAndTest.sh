testNumber="08"
javac src/* out/
java out/resource-manager --verbose testing/input/input-$testNumber
cat testing/output/output-$testNumber
