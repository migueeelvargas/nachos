package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {

	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		// First, we need to disable machine interrupts
		boolean status = Machine.interrupt().disable();

		// Now, access the waitQueue
		waitQueue.add(KThread.currentThread());

		// Next, the lock is released
		conditionLock.release();

		// Sleep the current thread
		KThread.sleep();

		// restore the machine's state
		conditionLock.acquire();
		Machine.interrupt().restore(status);
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean status = Machine.interrupt().disable();
		KThread next = null;

		if(waitQueue.isEmpty())
			Machine.interrupt().restore(status);
		else {
			next = waitQueue.pop();
			next.ready();
		}
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		// boolean status = Machine.interrupt().disable();

		// // Loop through all of the queued items on the linked list
		// for (KThread waitThread : waitQueue)
		// 	waitThread.ready();

		// // Clear the list
		// waitQueue.clear();

		// // Restore the machine's state
		// Machine.interrupt().restore(status);

		while (!waitQueue.isEmpty())
			wake();

	}

	private Lock conditionLock;

	private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
}
