import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;


public class QuicksortTesting {
	// Variable Declarations
	static int 	median, size, runs,
				seed  = 3141592,
				cores = Runtime.getRuntime().availableProcessors();

	static int[] orginArray, ReferArray, SrtedArray;
			

	// Test Data Structure: { size , runs }
	static int[][] 	datasArray = new int[][] { 
		{     1000,2500}, {    1778,2500}, {    3162,2500}, {    5623,2500}, 
		{    10000,2500}, {   17782,2500}, {   31622,1250}, {   56234,1250}, 
		{   100000, 750}, {  177827, 500}, {  316227, 250}, {  562341, 150}, 
		{  1000000, 100}, { 1778279,  75}, { 3162277,  60}, { 5623413,  50}, 
		{ 10000000,  40}, {17782794,  24}, {31622776,  16}, {56234132,   8}, 
		{100000000,   4}
	};
	
	static long startTimer, totalTimer;
	
	static long[][] timerArray;
	
	static String[] names = new String[] {
		"Arrays.Sort()", "QuicksortSequential", "QuicksortParallelNaive",
		"QuicksortForkJoin", "QuicksortExecutor"
	};
	
	static boolean firstRun = true;
	
	
	public static void main(String[] args) {
		totalTimer = System.nanoTime();
		
		for (int i = 0; i < datasArray.length; i++) {
			size = datasArray[i][0];
			runs = datasArray[i][1];
			
			orginArray = new int[size];	
			timerArray = new long[names.length][runs];
			initArrays(seed); 	
			median = (int)runs/2;
			if (median > 0) median--;
			
			System.out.println("Size: " + size + " | Runs: " + runs + 
							" | Seed: " + seed + " | Cores: " + cores);
		
			computeArrays();
		}

		System.out.println("Done in: " + ((System.nanoTime()-totalTimer)/1000000000.0 + " s\n"));
	}
	
	static void computeArrays() {
		//-- Built-in Arrays.sort() computation, used as referanse --
		for (int i = 0; i < runs; i++) {
			ReferArray = orginArray.clone();
			startTimer = System.nanoTime();
			Arrays.sort(ReferArray);
			timerArray[0][i] = System.nanoTime()-startTimer;
			//System.out.println((timerArray[0][i]/1000000.0) + " ms");
		}
		try {
			sortAndWrite(0);
		} catch (IOException e) {}
		
		
		//-- Sequential Quicksort computation --
		for (int i = 0; i < runs; i++) {
			SrtedArray = orginArray.clone();
			startTimer = System.nanoTime();
			QuickSeq QSeq = new QuickSeq();
			QSeq.quicksort(SrtedArray, 0, SrtedArray.length-1);
			timerArray[1][i] = System.nanoTime()-startTimer;
			//System.out.println((timerArray[1][i]/1000000.0) + " ms");
		}
		try {
			sortAndWrite(1);
		} catch (IOException e) {}
		
	
		
		//-- Parallel Naive Quicksort computation --
		for (int i = 0; i < runs; i++) {
			SrtedArray = orginArray.clone();
			startTimer = System.nanoTime();
			Thread QParNaive = new Thread(new QuickParNaive(SrtedArray, 0, SrtedArray.length-1));
			QParNaive.start();
			try {
				QParNaive.join();
			} catch (InterruptedException e) {}
			timerArray[2][i] = System.nanoTime()-startTimer;
			//System.out.println((timerArray[2][i]/1000000.0) + " ms");
		}
		try {
			sortAndWrite(2);
		} catch (IOException e) {}
		
		
		//-- Parallel Fork/Join Quicksort computation --
		ForkJoinPool QParFJ = new ForkJoinPool(cores);
		for (int i = 0; i < runs; i++) {
			SrtedArray = orginArray.clone();
			startTimer = System.nanoTime();
			QParFJ.invoke(new QuickParFJ(SrtedArray, 0, SrtedArray.length-1));
			timerArray[3][i] = System.nanoTime()-startTimer;
			//System.out.println((timerArray[3][i]/1000000.0) + " ms");
		}
		try {
			sortAndWrite(3);
		} catch (IOException e) {}
	
		
		//-- Parallel ExecutorService Quicksort computation --
		for (int i = 0; i < runs; i++) {
			SrtedArray = orginArray.clone();
			startTimer = System.nanoTime();
			final ExecutorService pool = Executors.newFixedThreadPool(cores);
			List<Future> futures = new Vector<Future>();
			QuickParExec QParExec = new QuickParExec(SrtedArray, 0, SrtedArray.length-1, futures, pool);
			futures.add(pool.submit(QParExec));
			while(!futures.isEmpty()) {
				Future topFeature = futures.remove(0);
				try {
					if(topFeature!=null)topFeature.get();
				} catch(InterruptedException ie) {} 
				  catch(ExecutionException ie)   {}
			}
			pool.shutdown();
			timerArray[4][i] = System.nanoTime()-startTimer;
			//System.out.println((timerArray[4][i]/1000000.0) + " ms");
		}
		try {
			sortAndWrite(4);
		} catch (IOException e) {}
		
		
		System.out.println();
		firstRun = false;
	}
	
	static void initArrays(int seed) {
		Random r = new Random(seed);
		
		for (int i = 0; i < orginArray.length; i++) {
			orginArray[i] = r.nextInt(orginArray.length);
		} 
	}
	
	static void sortAndWrite(int id) throws IOException {
		String fileName = names[id] + ".txt";
		Writer out = new OutputStreamWriter(new FileOutputStream(fileName, !firstRun));
		
		Arrays.sort(timerArray[id]);
		
		try {
			String s = Long.toString(timerArray[id][median]);
			out.write(size + "\t" + runs + "\t" + s + "\n");
		} finally { out.close(); }
		
		compareAndPrint(id);
	}
	
	static void compareAndPrint(int id) {
		double time = timerArray[id][median]/1000000.0;
		BigDecimal bd = new BigDecimal(time);
		
		if (time >= 1000)	  bd = bd.setScale(0, BigDecimal.ROUND_UP);
		else if (time >= 100) bd = bd.setScale(1, BigDecimal.ROUND_UP);
		else if (time >= 10)  bd = bd.setScale(2, BigDecimal.ROUND_UP);
		else bd = bd.setScale(3, BigDecimal.ROUND_UP);
		
		if (id == 0) System.out.print(names[id] + "\t\t: " + bd.toString() + " ms");
		else System.out.print(names[id] + "\t: " + bd.toString() + " ms");
		
		if (id > 0) {
			bd = new BigDecimal((timerArray[0][median]*1.0/timerArray[id][median]*1.0) * 100);
			bd = bd.setScale(1, BigDecimal.ROUND_UP);
	
			System.out.print("\tspeed: " + bd.toString() + " %");
			System.out.print((Arrays.equals(SrtedArray, ReferArray) ? "\n" : "\t!Error: not identical to referance!\n"));
		}
		
		else System.out.print("\n");
	}
		
}


// =============== // Sequential Quicksort implementation // ================ //
class QuickSeq {	
	int swaps = 0,	// Variables to track number of
		reads = 0,	//	swaps, reads and recursive QS-call,
		qscll = 0;	//  uncomment in code to use.
	final static int INSERTION_SORT_THRESHOLD = 47;
					
	
	void quicksort(int[] array, int left, int right) {
		//qscll++;	
		if (right-left <= INSERTION_SORT_THRESHOLD) {
			// do insertion sort when it is only 47 or less elements
			for (int i = left + 1; i <= right; i++) {
				int a = array[i];
				int j;
				for (j = i - 1; j >= left && a < array[j]; j--) {
					array[j + 1] = array[j];
				}
				array[j + 1] = a;
			}
		}
		
		else {
			int pivotIndex = partition(array, left, right);
			quicksort(array, left,  pivotIndex - 1);
			quicksort(array, pivotIndex + 1, right);
		}
		
		/*
		if (right == array.length-1 && left == 0) {
			System.out.println("Swaps: " + swaps); 
			System.out.println("Reads: " + reads);
			System.out.println("Qscll: " + qscll);
		}
		*/
	}
	
	int partition(int[] array, int left, int right) {	
		int pivotValue = array[(left + right) / 2];
		swap(array, (left + right) / 2, right); 
		int index = left;
		
		for (int i = left; i < right; i++) {
			//reads++;
			if (array[i] <= pivotValue) {
				swap(array, i, index);
				index++;
			}
		}
		
		swap(array, index, right);
		return index;
	}
	
	void swap(int[] array, int left, int right) {
		//swaps++;
		int temp = array[left];
		array[left] = array[right];
		array[right] = temp;
	}
}
// =====END======= // Sequential Quicksort implementation // ======END======= //


// ============= // Parallel Naive Quicksort implementation // ============== //
class QuickParNaive implements Runnable {
	private final int[] array;
	private final int left, right;
	final static int LIMIT = 50000;	// Used to not start new threads where right-left < LIMIT
	final static int INSERTION_SORT_THRESHOLD = 47;
	
	
	public QuickParNaive(int[] arr, int l, int r) { 
		this.array = arr;
		this.left  = l;
		this.right = r;
	}
	
	public void run() {
		quicksort(array, left, right); 
	}
	
	void quicksort(int[] array, int left, int right) {
		if (right-left <= INSERTION_SORT_THRESHOLD) {
			for (int i = left + 1; i <= right; i++) {
				int a = array[i];
				int j;
				for (j = i - 1; j >= left && a < array[j]; j--) {
					array[j + 1] = array[j];
				}
				array[j + 1] = a;
			}
		}
		
		else {
			int pivotIndex = partition(array, left, right);
		
			if (right-left <= LIMIT) {
				quicksort(array, left,  pivotIndex - 1);
				quicksort(array, pivotIndex + 1, right);
			}
			
			else {
				Thread t1 = new Thread(new QuickParNaive(array, left,  pivotIndex - 1));
				Thread t2 = new Thread(new QuickParNaive(array, pivotIndex + 1, right));
				t1.start();
				t2.start();
				
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException e) { return; }
			}
		}
	}
	
	int partition(int[] array, int left, int right) {
		int pivotValue = array[(left + right) / 2];
		swap(array, (left + right) / 2, right);
		int index = left;
		
		for (int i = left; i < right; i++) {
			if (array[i] <= pivotValue) {
				swap(array, i, index);
				index++;
			}
		}
		
		swap(array, index, right);
		return index;
	}
	
	void swap(int[] array, int left, int right) {
		int temp = array[left];
		array[left] = array[right];
		array[right] = temp;
	}
}
// =====END===== // Parallel Naive Quicksort implementation // =====END====== //


// ======== // Parallel ExecutorService Quicksort implementation // ========= //
class QuickParExec implements Runnable { 	
	private final int[] array;
	private final int left, right;
	final static int LIMIT = 50000;
	final static int INSERTION_SORT_THRESHOLD = 47;
	ExecutorService pool;
	List<Future> futurelist;
	
	public QuickParExec(int[] arr, int left, int right, List<Future> futurelist, ExecutorService pool) { 
		this.array = arr;
		this.left = left;
		this.right = right;
		this.futurelist = futurelist;
		this.pool = pool;
	}
	
	public void run() { 
		quicksort(array, left, right);
	}
	
	void quicksort(int[] array, int left, int right) {
		if (right-left <= INSERTION_SORT_THRESHOLD) {
			for (int i = left + 1; i <= right; i++) {
				int a = array[i];
				int j;
				for (j = i - 1; j >= left && a < array[j]; j--) {
					array[j + 1] = array[j];
				}
				array[j + 1] = a;
			}
		}
	
		else {
			int pivotIndex = partition(left, right);
			
			if (right-left <= LIMIT) {
				quicksort(array, left,  pivotIndex - 1);
				quicksort(array, pivotIndex + 1, right);
			}
			else { 
				futurelist.add(pool.submit(new QuickParExec(array, left,  pivotIndex - 1, futurelist, pool)));
				futurelist.add(pool.submit(new QuickParExec(array, pivotIndex + 1, right, futurelist, pool)));
			}
		}
	}
	
	int partition(int left, int right) {	
		int pivotValue = array[(left + right) / 2];
		swap(array, (left + right) / 2, right);
		int index = left;
		
		for (int i = left; i < right; i++) {
			if (array[i] <= pivotValue) {
				swap(array, i, index);
				index++;
			}
		}
		
		swap(array, index, right);
		return index;
	}
	
	void swap(int[] array, int left, int right) {
		int temp = array[left];
		array[left] = array[right];
		array[right] = temp;
	}
}
// ==END=== // Parallel ExecutorService Quicksort implementation // ===END=== //


// =========== // Parallel Fork/join Quicksort implementation // ============ //
class QuickParFJ extends RecursiveAction { 
	private final int[] array;
	private final int left, right;
	final static int LIMIT = 50000;
	final static int INSERTION_SORT_THRESHOLD = 47;
	
	public QuickParFJ(int[] arr, int left, int right) { 
		this.array = arr;
		this.left = left;
		this.right = right;
	}
	
	@Override
	protected void compute() {
		quicksort(array, left, right);
	}
	
	void quicksort(int[] array, int left, int right) {
		if (right-left <= INSERTION_SORT_THRESHOLD) {
			for (int i = left + 1; i <= right; i++) {
				int a = array[i];
				int j;
				for (j = i - 1; j >= left && a < array[j]; j--) {
					array[j + 1] = array[j];
				}
				array[j + 1] = a;
			}
		}
	
		else  {
			int pivotIndex = partition(array, left, right);
			
			if (right-left <= LIMIT) {
				quicksort(array, left,  pivotIndex - 1);
				quicksort(array, pivotIndex + 1, right);
			}
			else {
				invokeAll(	new QuickParFJ(array, left,  pivotIndex - 1),
							new QuickParFJ(array, pivotIndex + 1, right));
			}
		}
	}
	
	int partition(int[] array, int left, int right) {	
		int pivotValue = array[(left + right) / 2];
		swap(array, (left + right) / 2, right);
		int index = left;
		
		for (int i = left; i < right; i++) {
			if (array[i] <= pivotValue) {
				swap(array, i, index);
				index++;
			}
		}
		
		swap(array, index, right);
		return index;
	}
	
	void swap(int[] array, int left, int right) {
		int temp = array[left];
		array[left] = array[right];
		array[right] = temp;
	}
}
// ====END==== // Parallel Fork/join Quicksort implementation // ====END===== //
