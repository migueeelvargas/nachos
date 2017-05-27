package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */

	public Alarm() {
		// Allocate new threads.
		// Using TreeMap to keep track of times as key & their KThread.
		sleepThreads = new TreeMap<Long, KThread>();

		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {

		// Needed for the atomic op
		boolean status = Machine.interrupt().disable();

		// Loop gets all the threads with expired wait times
		// Checks if Map is not empty & if the lowest key in the map's time (long)
		// is less than the Machine's current time
		while(!sleepThreads.isEmpty() && 
			sleepThreads.firstKey() <= Machine.timer().getTime()) {

			// If so, it will make that KThread ready
			// This will take the lowest key (time) & remove it from the map. 
			sleepThreads.pollFirstEntry().getValue().ready();
		}
		
		// Restore
		Machine.interrupt().restore(status);

		// Yield
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {

		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;

		// Atomic op
		boolean status = Machine.interrupt().disable();

		// Now place the thread in the waiting map.
		sleepThreads.put(wakeTime, KThread.currentThread());
		KThread.sleep();

		Machine.interrupt().restore(status);
	}

	// Create priority queue needed for sleep threads
	private TreeMap<Long, KThread> sleepThreads;
}
