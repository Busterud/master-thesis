:: Maximum number of mechanisms to test using Powers of Two (2^n)
::  32 will test: { 1 , 2 , 4 , 8 , 16 , 32 }
SET /a numberOfMechanisms = 32

:: Number of test runs, more runs result in more precise output
SET /a numberOfTestRun = 2000

:: Compile Java-files
javac OverheadTesting.java
javac OverheadOutput.java

SET /a i = 1
:outerLoop
:: GTR means Greater than (>)
IF %i% GTR %numberOfMechanisms% GOTO outerEnd
	SET /a j = 1
	:innerLoop
	IF %j% GTR %numberOfTestRun% GOTO innerEnd
		:: Start the overhead testing with current 
		::  number of mechanisms to test with as parameter
		java OverheadTesting %i%
		SET /a j = %j% + 1
	GOTO innerLoop
	:innerEnd
	SET /a i = %i% * 2
GOTO outerLoop
:outerEnd

:: Output results to LaTeX and graphs
java OverheadOutput %numberOfTestRun%
echo Test run done.