#! /bin/bash

# Maximum number of mechanisms to test using Powers of Two (2^n)
#  32 will test: { 1 , 2 , 4 , 8 , 16 , 32 }
numberOfMechanisms=32

# Number of test runs, more runs result in more precise output
numberOfTestRun=2000

# Compile Java-files
javac OverheadTesting.java
javac OverheadOutput.java

i=1
# -le means Less than or equal (<=)
while [ $i -le $numberOfMechanisms ]; do
	j=1
	while [ $j -le $numberOfTestRun ]; do
		# Start the overhead testing with current
		#  number of mechanisms to test with as parameter
		java OverheadTesting $i
		let j+=1
	done
	let i*=2
done

# Output results to LaTeX and graphs
java OverheadOutput $numberOfTestRun
echo Test run done.