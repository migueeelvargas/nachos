package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {

		byte[] mem = Machine.processor().getMemory();
		int bytes = 0;

		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= mem.length) {
			return 0;
		}

    bytes = Math.min(length, mem.length - vaddr);

    System.arraycopy(mem, vaddr, data, offset, bytes);

		// while (offset < data.length && length > 0) {

		// 	int addOff = vaddr % 1024;
		// 	int vPage = vaddr / 1024;

  //     // Check for vPage
		// 	if (vPage < 0 || vPage >= pageTable.length) {
		// 		break;
		// 	}

		// 	TranslationEntry curr = pageTable[vPage];

		// 	// Check if current entry is valid if not break
		// 	if (!curr.valid){
		// 		break;
		// 	}

		// 	// Assign to true
		// 	curr.used = true;

		// 	// Find transfer size
		// 	int pPage = pte.ppn;
		// 	int pAddress = (pPage * 1024) + addOff;
		// 	int tSize = Math.min((data.length - offset), Math.min(length, (1024 - addOff)));

  //     // Copy array
		// 	System.arraycopy(mem, pAddress, data, offset, tSize);

		// 	// Update vars
		// 	vaddr = vaddr + tSize;
		// 	offset = offset + tSize;
		// 	length = length - tSize;
		// 	bytes = bytes + tSize;
		// }



		return bytes;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

    int bytes = 0;
		byte[] mem = Machine.processor().getMemory();

    while (offset < data.length && length > 0) {

      int addOff = vaddr % 1024;
      int vPage = vaddr / 1024;

      if (vPage < 0 || vPage >= pageTable.length)
        return 0;

      TranslationEntry curr = pageTable[vPage];

      // Check if valid or read only
      if (!curr.valid || curr.readOnly){
        break;
      }

      // Assign to true
      curr.dirty = true;
      curr.used = true;

      // Find transfer size
      int pPage = curr.ppn;
      int pAddress = (pPage * 1024) + addOff;
      int tSize = Math.min(data.length - offset, Math.min(length, (1024 - addOff)));

      // Copy array
      System.arraycopy(mem, pAddress, data, offset, tSize);

      // Update vars
      vaddr = vaddr + tSize;
      offset = offset + tSize;
      length = length - tSize;
      bytes = bytes + tSize;
    }

		return bytes;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, vpn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private static final int 
    syscallHalt = 0, 
    syscallExit = 1, 
    syscallExec = 2,
		syscallJoin = 3, 
    syscallCreate = 4, 
    syscallOpen = 5,
		syscallRead = 6, 
    syscallWrite = 7,
    syscallClose = 8,
		syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

  /* FILE MANAGEMENT SYSCALLS: creat, open, read, write, close, unlink
   *
   * A file descriptor is a small, non-negative integer that refers to a file on
   * disk or to a stream (such as console input, console output, and network
   * connections). A file descriptor can be passed to read() and write() to
   * read/write the corresponding file/stream. A file descriptor can also be
   * passed to close() to release the file descriptor and any associated
   * resources.
   */

  /**
   * Attempt to open the named disk file, creating it if it does not exist,
   * and return a file descriptor that can be used to access the file.
   *
   * Note that creat() can only be used to create files on disk; creat() will
   * never return a file descriptor referring to a stream.
   *
   * Returns the new file descriptor, or -1 if an error occurred.
   */

  private int creat(int nameAddress) {

    try {
      // read from virtual memory & attach info
      String nameOfFile = readVirtualMemoryString(nameAddress, Machine.processor().getMemory().length - nameAddress - 1);

      // check if the file is empty or if in the unlinked list
      if (unlinked.contains(nameOfFile) || nameOfFile == null) {
        return -1;
      }

      // Check if file is in the global hashmap of files
      if (files.containsKey(nameOfFile)) {

        if (files.get(nameOfFile).containsKey(this)) {
          return Arrays.asList(files.values().toArray()).indexOf(files.get(nameOfFile));
        }

        // If something went wrong, exit 
        return -1;
      }

      // Get the file size
      int size = files.size();

      // Create hashmap that is used to put in the files hashmap
      HashMap<UserProcess, OpenFile> value = new HashMap<UserProcess, OpenFile>();

      // Create OpenFile object
      OpenFile file = ThreadedKernel.fileSystem.open(nameOfFile, true);

      // Add the object to the recently created map
      value.put(this, file);

      // Add key & value to the global files hashmap
      files.put(nameOfFile, value);

      // return the size
      return size;    
    }
    // If any errors are caught
    catch (Exception e) {
      return -1;
    }

  }

  /**
   * Attempt to open the named file and return a file descriptor.
   *
   * Note that open() can only be used to open files on disk; open() will never
   * return a file descriptor referring to a stream.
   *
   * Returns the new file descriptor, or -1 if an error occurred.
   */

  private int open(int nameAddress){

    try {

      // read from virtual memory & attach info
      String nameOfFile = readVirtualMemoryString(nameAddress, Machine.processor().getMemory().length - nameAddress - 1);

      // check if the file is empty or if in the unlinked list
      if (unlinked.contains(nameOfFile) || nameOfFile == null) {
        return -1;
      }

      // Check if file is in the global hashmap of files
      if (files.containsKey(nameOfFile)) {

        if (files.get(nameOfFile).containsKey(this)) {
          return Arrays.asList(files.values().toArray()).indexOf(files.get(nameOfFile));
        }

        // If something went wrong, exit 
        return -1;
      }

      // Get the file size
      int size = files.size();

      // Create OpenFile object
      OpenFile file = ThreadedKernel.fileSystem.open(nameOfFile, true);

      // Check if the file returned is null
      // if so, exit
      if (file == null) {
        return -1;
      }

      // Create hashmap that is used to put in the files hashmap
      HashMap<UserProcess, OpenFile> value = new HashMap<UserProcess, OpenFile>();

      // Add the object to the recently created map
      value.put(this, file);

      // Add key & value to the global files hashmap
      files.put(nameOfFile, value);

      // return the size
      return size;
    }
    // If any errors are caught
    catch (Exception e){
      return -1;
    }
  }


  /**
   * Attempt to read up to count bytes into buffer from the file or stream
   * referred to by fileDescriptor.
   *
   * On success, the number of bytes read is returned. If the file descriptor
   * refers to a file on disk, the file position is advanced by this number.
   *
   * It is not necessarily an error if this number is smaller than the number of
   * bytes requested. If the file descriptor refers to a file on disk, this
   * indicates that the end of the file has been reached. If the file descriptor
   * refers to a stream, this indicates that the fewer bytes are actually
   * available right now than were requested, but more bytes may become available
   * in the future. Note that read() never waits for a stream to have more data;
   * it always returns as much as possible immediately.
   *
   * On error, -1 is returned, and the new file position is undefined. This can
   * happen if fileDescriptor is invalid, if part of the buffer is read-only or
   * invalid, or if a network stream has been terminated by the remote host and
   * no more data is available.
   */

  private int read(int fileDescriptor, int buffer, int count) {

    try {

      OpenFile file = ((HashMap<UserProcess, OpenFile>) Arrays.asList(files.values().toArray()).get(fileDescriptor)).get(this);

      String nameOfFile = (String) Arrays.asList(files.keySet().toArray()).get(fileDescriptor);

      if (mmaps.contains(nameOfFile)) {
        return -1;
      }

      byte[] data = new byte[count];

      int bytesRead = file.read(data, 0, count);

      writeVirtualMemory(buffer, data);

      return bytesRead;

    }
    // If any errors caught
    catch (Exception e) {
      return -1;
    }
  }

  /**
   * Attempt to write up to count bytes from buffer to the file or stream
   * referred to by fileDescriptor. write() can return before the bytes are
   * actually flushed to the file or stream. A write to a stream can block,
   * however, if kernel queues are temporarily full.
   *
   * On success, the number of bytes written is returned (zero indicates nothing
   * was written), and the file position is advanced by this number. It IS an
   * error if this number is smaller than the number of bytes requested. For
   * disk files, this indicates that the disk is full. For streams, this
   * indicates the stream was terminated by the remote host before all the data
   * was transferred.
   *
   * On error, -1 is returned, and the new file position is undefined. This can
   * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
   * if a network stream has already been terminated by the remote host.
   */

  private int write(int fileDescriptor, int buffer, int count) {
    try {
      OpenFile file = ((HashMap<UserProcess, OpenFile>) Arrays.asList(files.values().toArray()).get(fileDescriptor)).get(this);

      String nameOfFile = (String) Arrays.asList(files.keySet().toArray()).get(fileDescriptor);

      if (mmaps.contains(nameOfFile)) {
        return -1;
      }

      byte[] data = new byte[count];

      readVirtualMemory(buffer, data, 0, count);

      int bytesWritten = file.write(data, 0, count);

      return bytesWritten;
    }
    // If any errors are caught return -1
    catch (Exception e) {
      return -1;
    }
  }

  /**
   * Close a file descriptor, so that it no longer refers to any file or stream
   * and may be reused.
   *
   * If the file descriptor refers to a file, all data written to it by write()
   * will be flushed to disk before close() returns.
   * If the file descriptor refers to a stream, all data written to it by write()
   * will eventually be flushed (unless the stream is terminated remotely), but
   * not necessarily before close() returns.
   *
   * The resources associated with the file descriptor are released. If the
   * descriptor is the last reference to a disk file which has been removed using
   * unlink, the file is deleted (this detail is handled by the file system
   * implementation).
   *
   * Returns 0 on success, or -1 if an error occurred.
   */

  private int close(int fileDescriptor) {

    try {

      // Obtain the file info from hashmap
      OpenFile file = ((HashMap<UserProcess, OpenFile>) Arrays.asList(files.values().toArray()).get(fileDescriptor)).get(this);

      // Close the file
      file.close();

      // Get name of file
      String nameOfFile = (String) Arrays.asList(files.keySet().toArray()).get(fileDescriptor);

      // Remove from global hashmap
      files.remove(nameOfFile);

      // Success
      return 0;
    }
    // If any errors are caught return -1
    catch (Exception e) {
      return -1;
    }
  }

  /**
   * Delete a file from the file system. If no processes have the file open, the
   * file is deleted immediately and the space it was using is made available for
   * reuse.
   *
   * If any processes still have the file open, the file will remain in existence
   * until the last file descriptor referring to it is closed. However, creat()
   * and open() will not be able to return new file descriptors for the file
   * until it is deleted.
   *
   * Returns 0 on success, or -1 if an error occurred.
   */

  private int unlink(int fileDescriptor) {

    try {

      // Obtain the file from hashmap
      OpenFile file = ((HashMap<UserProcess, OpenFile>) Arrays.asList(files.values().toArray()).get(fileDescriptor)).get(this);

      // Get name of file
      String nameOfFile = (String) Arrays.asList(files.keySet().toArray()).get(fileDescriptor);

      // Remove from global hashmap
      files.remove(nameOfFile);

      // Success
      return 0;
    }
    // If any errors are caught return -1
    catch (Exception e) {
      return -1;
    }
  }

  /**
   * Map the file referenced by fileDescriptor into memory at address. The file
   * may be as large as 0x7FFFFFFF bytes.
   * 
   * To maintain consistency, further calls to read() and write() on this file
   * descriptor will fail (returning -1) until the file descriptor is closed.
   *
   * When the file descriptor is closed, all remaining dirty pages of the map
   * will be flushed to disk and the map will be removed.
   *
   * Returns the length of the file on success, or -1 if an error occurred.
   */

  private int mmap(int fileDescriptor, int address) {

    // Declare empty string
    String nameOfFile = "";

    try {

      // Obtain the file from hashmap
      OpenFile file = ((HashMap<UserProcess, OpenFile>) Arrays.asList(files.values().toArray()).get(fileDescriptor)).get(this);

      // Get name of file
      nameOfFile = (String) Arrays.asList(files.keySet().toArray()).get(fileDescriptor);

      mmaps.add(nameOfFile);

      int size = 0;
      int bytesRead = 0;
      byte[] data = new byte[1024];

      do {
        bytesRead = file.read(data, 0, 1024);
        writeVirtualMemory(address, data);

        address = address + bytesRead;
        size = size + bytesRead;
      }

      while (bytesRead == 1024);

      return size;
    }
    // If any errors caught, return -1
    catch (Exception e) {
      return -1;
    }
    finally {
      mmaps.remove(nameOfFile);
    }


  }



	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';


  // private lists
  private LinkedList<Condition> cQueue = new LinkedList<Condition>();
  private LinkedList<Lock> lQueue = new LinkedList<Lock>();
  private LinkedList<Semaphore> sQueue = new LinkedList<Semaphore>();
  
  // public lists
  public static LinkedHashMap<String, HashMap<UserProcess, OpenFile>> files = new LinkedHashMap<String, HashMap<UserProcess, OpenFile>>();
  public static LinkedList<String> unlinked = new LinkedList<String>();
  public static LinkedList<String> mmaps = new LinkedList<String>();

}
