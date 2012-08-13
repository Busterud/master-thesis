import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

public class OverheadTesting {

	static boolean newFile = false;		
	// false = append to current file (if exist)
	
	static int 	currentThreadNr,
				numberOfThreads,
				dummy = 0;
			
	static long	startRunTime,
				doneRunTime;
	
	static long[]	startCreateTime,
					doneCreateTime,
					startCallTime,
					doneCallTime;
					
	static Random r;
	
	static double[] timeArray = new double[7];
	// timeArray[] contains following elements:
	// 0: firstCreateTime | 1: firstCallTime | 2: totCreateTime | 
	// 3: totCallTime 	  | 4: avgCreateTime | 5: avgCallTime   | 6: totAllTime (4+5)
	
	static String[] names = new String[] {
		"MethodCall", "ClassCall", "ThreadCall", "ExecutorCall", "ForkJoinCall"
	};
	
	public static void main(String[] args) {
		if (args.length == 0)  { 
			System.out.println("Need arguments, use:\n\"java OverheadTesting *numberOfThreads*\""); 
			return; 
		}
		
		r = new Random(3141592);
		
		if (args.length > 0) numberOfThreads = Integer.parseInt(args[0]);
		if (args.length > 1) newFile = Boolean.parseBoolean(args[1]);
		
		OverheadTesting OT = new OverheadTesting();
		
		// Arrays storing start and completion time for Creating and Calling the different mechanisms
		startCreateTime = new long[numberOfThreads];
		doneCreateTime  = new long[numberOfThreads];
		startCallTime 	= new long[numberOfThreads];
		doneCallTime 	= new long[numberOfThreads];
		
		
		//-- MethodCall; calling (ID: 0)
		startRunTime = System.nanoTime();
		for (int i = 0; i < numberOfThreads; i++) {
			startCallTime[currentThreadNr] = System.nanoTime();
			MetodeCall();
		}
		doneRunTime = System.nanoTime();
		
		try {
			writeToFile(0);
		} catch (IOException e) {}
		
		
		//-- ClassCall; creating and calling (ID: 1)
		currentThreadNr = 0;
		
		startRunTime = System.nanoTime();
		startCreateTime[currentThreadNr] = System.nanoTime();
		ClassCall mainClass = OT.new ClassCall();
		doneCreateTime[currentThreadNr] = System.nanoTime();
		
		startCallTime[currentThreadNr] = System.nanoTime();
		mainClass.run();
		doneRunTime = System.nanoTime();
		
		try {
			writeToFile(1);
		} catch (IOException e) {}
		 
		
		//-- ThreadCall; creating and calling (ID: 2)
		currentThreadNr = 0;
		
		startRunTime = System.nanoTime();
		startCreateTime[currentThreadNr] = System.nanoTime();
		Thread mainThread = new Thread(OT.new ThreadCall());
		doneCreateTime[currentThreadNr] = System.nanoTime(); 
		
		startCallTime[currentThreadNr] = System.nanoTime();
		mainThread.start();
		try { 
			mainThread.join(); 
		} catch (InterruptedException e) {}
		doneRunTime = System.nanoTime();
		
		try {
			writeToFile(2);
		} catch (IOException e) {}
		
		
		//-- ExecutorCall; creating and calling (ID: 3)
		currentThreadNr = 0;
		
		startRunTime = System.nanoTime();
		startCreateTime[currentThreadNr] = System.nanoTime();
		final ExecutorService execPool = Executors.newFixedThreadPool(numberOfThreads);
		List<Future> futurelist = new Vector<Future>();
		ExecutorCall mainExecutor = OT.new ExecutorCall(futurelist, execPool);
		doneCreateTime[currentThreadNr] = System.nanoTime(); 
		
		startCallTime[currentThreadNr] = System.nanoTime();
		execPool.execute(mainExecutor);
		futurelist.add(execPool.submit(mainExecutor));
		while(!futurelist.isEmpty()) {
			Future topFeature = futurelist.remove(0);
			try {
				if(topFeature!=null)topFeature.get();
			} catch(InterruptedException ie) {} 
			  catch(ExecutionException ie)   {}
		}
		doneRunTime = System.nanoTime();
		
		try {
			writeToFile(3);
		} catch (IOException e) {}
		
		
		//-- ForkJoin; creating and calling (id: 4)
		currentThreadNr = 0;
		
		startRunTime = System.nanoTime();
		startCreateTime[currentThreadNr] = System.nanoTime();
		ForkJoinPool fjPool = new ForkJoinPool(numberOfThreads);
		ForkJoinCall mainFJ = OT.new ForkJoinCall();
		doneCreateTime[currentThreadNr] = System.nanoTime();
		
		startCallTime[currentThreadNr] = System.nanoTime();
		fjPool.invoke(mainFJ);
		doneRunTime = System.nanoTime();
		
		try {
			writeToFile(4);
		} catch (IOException e) {}
			
		System.out.println(dummy);
		//System.out.println("Testing complete and appended results to file.");
		System.exit(0);
	}
	
	// ===== // Method-Call // ===== //
	static void MetodeCall() { 
		dummy++;
		doneCallTime[currentThreadNr++] = System.nanoTime();
		double checksum = ((System.nanoTime()+currentThreadNr)*r.nextInt(1000)/System.currentTimeMillis());
		// Have to create a randomvalue so Java does NOT optimize
		// System.out.println("Checksum: " + checksum);
	}
	
	// ===== // Regular-Class // ===== //
	public class ClassCall {
		public void run() { 
			dummy++;
			doneCallTime[currentThreadNr] = System.nanoTime();
			if (currentThreadNr+1 < numberOfThreads) {
				int i = ++currentThreadNr;
				dummy++;
				
				startCreateTime[i] = System.nanoTime();
				ClassCall subClass = new ClassCall();
				doneCreateTime[i] = System.nanoTime();
				
				startCallTime[i] = System.nanoTime();
				subClass.run();
			}
			double checksum = ((System.nanoTime()+currentThreadNr)*r.nextInt(1000)/System.currentTimeMillis());
			// Have to create a randomvalue so Java does NOT optimize
			// System.out.println("Checksum: " + checksum);
		}
	}
	
	// ===== // Thread-Class // ===== //
	public class ThreadCall implements Runnable {
		public void run() {
			dummy++;
			doneCallTime[currentThreadNr] = System.nanoTime();
			if (currentThreadNr+1 < numberOfThreads) {
				int i = ++currentThreadNr;
				dummy++;
				
				startCreateTime[i] = System.nanoTime();
				Thread subThread = new Thread(new ThreadCall());
				doneCreateTime[i] = System.nanoTime();
				
				startCallTime[i] = System.nanoTime();
				subThread.start();
				try { 
					subThread.join(); 
				} catch (InterruptedException e) {}
			}
			double checksum = ((System.nanoTime()+currentThreadNr)*r.nextInt(1000)/System.currentTimeMillis());
		}
	}
	
	// ===== // ExecutorService-Class // ===== //
	public class ExecutorCall implements Runnable { 
		List<Future> futurelist;
		ExecutorService pool;
		
		public ExecutorCall(List<Future> fl, ExecutorService pl) {
			this.futurelist = fl;
			this.pool = pl;
		}
	
		public void run() { 
			dummy++;
			doneCallTime[currentThreadNr] = System.nanoTime();
			if (currentThreadNr+1 < numberOfThreads) {
				int i = ++currentThreadNr;
				dummy++;
				
				startCreateTime[i] = System.nanoTime();
				ExecutorCall subExecutor = new ExecutorCall(futurelist, pool);
				doneCreateTime[i] = System.nanoTime();
				
				startCallTime[i] = System.nanoTime();
				futurelist.add(pool.submit(subExecutor));			
			}
			double checksum = ((System.nanoTime()+currentThreadNr)*r.nextInt(1000)/System.currentTimeMillis());
		}	
	}

	// ===== // ForkJoin-Class // ===== //
	public class ForkJoinCall extends RecursiveAction { 
		@Override
		protected void compute() { 
			dummy++;
			doneCallTime[currentThreadNr] = System.nanoTime();
			if (currentThreadNr+1 < numberOfThreads) {
				int i = ++currentThreadNr;
				dummy++;
				
				startCreateTime[i] = System.nanoTime();
				ForkJoinCall subFJ = new ForkJoinCall();
				doneCreateTime[i] = System.nanoTime();
				
				startCallTime[i] = System.nanoTime();
				invokeAll(subFJ);
			}
			double checksum = (System.nanoTime()+currentThreadNr*r.nextInt(1000)/System.currentTimeMillis());
		}
	}

	
	// Method to write the results to file
	static void writeToFile(int id) throws IOException {
		String fileName = names[id] + ".txt";
		String output = ("" + numberOfThreads);
		Writer out = new OutputStreamWriter(new FileOutputStream(fileName, !newFile));
		
		// Calculate times for the output
		timeArray[0] = (doneCreateTime[0]-startCreateTime[0])/1000.0; // First Creation Time in microseconds
		timeArray[1] = (doneCallTime[0]-startCallTime[0])/1000.0; 	  // First Call Time in microseconds
		
		if (numberOfThreads > 1) {
			// Uncomment if printTimes() are not used
			timeArray[4] = 0;
			timeArray[5] = 0;
			for (int i = 0; i < numberOfThreads; i++) {
				timeArray[4] += (doneCreateTime[i]-startCreateTime[i])/1000.0;
				timeArray[5] += (doneCallTime[i]-startCallTime[i])/1000.0;
			}
		
			timeArray[2] = (timeArray[4]-((doneCreateTime[0]-startCreateTime[0])/1000.0) )/(numberOfThreads-1);	// Additionnel Creation Time in microseconds
			timeArray[3] = (timeArray[5]-((doneCallTime[0]-startCallTime[0])/1000.0) )/(numberOfThreads-1);	// Additionnel Call Time in microseconds
			
			timeArray[6] = timeArray[4]+timeArray[5];
		}
		
		else {
			for (int i = 2; i < timeArray.length; i++) {
				timeArray[i] = 0;
			}
			
			timeArray[6] = timeArray[0]+timeArray[1];
		}
		
		
		// Format the output
		for (int i = 0; i < timeArray.length; i++) {
			BigDecimal bd = new BigDecimal(timeArray[i]);
			
			if (timeArray[i] >= 1000)	  bd = bd.setScale(0, BigDecimal.ROUND_UP);
			else if (timeArray[i] >= 100) bd = bd.setScale(1, BigDecimal.ROUND_UP);
			else if (timeArray[i] >= 10)  bd = bd.setScale(2, BigDecimal.ROUND_UP);
			else bd = bd.setScale(3, BigDecimal.ROUND_UP);
			
			output += ("\t" + bd.toString());
		}
		output += ("\n");
		
		// write to file
		try {
			out.write(output);
		} finally { out.close(); }
	}
}	

