package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {

	// Initialize all variables & objects
	private Integer buff;
	private int ret_val;
	private Lock lock;
	private Condition rtn;
	private Condition speak;
	private Condition listen;
	
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {

		// Int buffer
		buff = null; 

		// Lock
		lock = new Lock();

		// Conditions
		rtn = new Condition(lock);		
		speak = new Condition(lock);
		listen = new Condition(lock);
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {

		// Acquire the lock
		lock.acquire();

		while (buff != null)
			speak.sleep();

		// Input the word into the buffer
		buff = word;

		// Wake the listener
		listen.wake();

		// Sleep the return condition
		rtn.sleep();

		// Release the lock
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {

		// acquire the lock
		lock.acquire();

		// if the buffer is empty
		while (buff == null)
			listen.sleep();

		// Store word from buffer & clear buffer
		ret_val = buff.intValue();
		buff = null;

		// Wake up the conditions
		speak.wake();
		rtn.wake();

		// Release the lock
		lock.release();

		return ret_val;
	}
}
