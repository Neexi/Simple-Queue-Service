package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * File based implementation of message queue service Using written time record
 * to keep in track the visibility period of a message For now the file is saved
 * on the same directory as the source code TODO : better directory
 * implementation, like using tmp folder
 * 
 * @author Rudi Purnomo
 *
 */
public class FileQueueService implements QueueService {

	private static String QUEUE_DIRECTORY = "file";
	// Path for default queue
	private static final String DEFAULT_QUEUE = "DEFAULT";
	private static final String LOCK_FILE = ".lock";
	private static final String MESSAGE_FILE = "message";
	private static final long DEFAULT_VISIBILITY_TIMEOUT = 3000;

	/**
	 * Create default queue if it does not exist at the moment TODO : Add other
	 * variable/functionality bonded to specific instance of the service
	 */
	public FileQueueService() {
		if (!isQueueExist(DEFAULT_QUEUE))
			createQueue(DEFAULT_QUEUE);
	}

	/**
	 * Push method for file based queue service. Works by appending the message
	 * in record format to the end of message file in the queue directory
	 */
	@Override
	public boolean push(String queue, Message message) {
		File messageFile = getMessageFile(queue);
		File lock = getQueueLock(queue);
		try {
			lock(lock);
			PrintWriter pw = new PrintWriter(new FileWriter(messageFile, true));
			pw.println(message.createVisibleRecord());
			pw.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			unlock(lock);
		}
		return true;
	}

	/**
	 * Push method without queue argument, this will push the message to default
	 * queue if it exists
	 * 
	 * @param message
	 *            the message
	 * @return true if push is successful, false otherwise
	 */
	public boolean push(Message message) {
		return push(DEFAULT_QUEUE, message);
	}

	/**
	 * Pull method for file based queue service. Works by creating temporary
	 * duplicate message file which will overwrite the original one at the end
	 * of the function. Duplicate file will have exactly the same content as the
	 * original one beside the first visible message line which now will be set
	 * as invisible
	 */
	@Override
	public Message pull(String queue) {
		Message ret = null;
		File lock = getQueueLock(queue);
		try {
			lock(lock);
			File messageFile = getMessageFile(queue);
			if (messageFile == null) {
				unlock(lock);
				return ret;
			}
			File tempFile = createTemporaryMessageFile(queue);
			BufferedReader br = new BufferedReader(new FileReader(messageFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
			String line;
			boolean found = false;
			while ((line = br.readLine()) != null) {
				Message curMessage = Message.createMessageFromRecord(line.trim());
				if (!found && curMessage.getVisiblility()) {
					curMessage.setInvisible();
					ret = curMessage;
					bw.write(curMessage.createInvisibleRecord(DEFAULT_VISIBILITY_TIMEOUT)
							+ System.getProperty("line.separator"));
					found = true;
					continue;
				}
				bw.write(line + System.getProperty("line.separator"));
			}
			bw.close();
			br.close();
			messageFile.delete();
			tempFile.renameTo(messageFile);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
			return ret;
		} finally {
			unlock(lock);
		}
		return ret;
	}

	/**
	 * Pull method without queue argument, this will pull message from default
	 * queue if it exists
	 * 
	 * @return the first visible message in the queue
	 */
	public Message pull() {
		return pull(DEFAULT_QUEUE);
	}

	/**
	 * Delete method for file based queue service. Works by creating temporary
	 * duplicate message file which will overwrite the original one at the end
	 * of the function. Duplicate file will have exactly the same content as the
	 * original one minus the message line which contains the record of input
	 * message
	 */
	@Override
	public boolean delete(String queue, Message message) {
		File lock = getQueueLock(queue);
		try {
			lock(lock);
			File messageFile = getMessageFile(queue);
			if (messageFile == null) {
				unlock(lock);
				return false;
			}
			File tempFile = createTemporaryMessageFile(queue);
			BufferedReader br = new BufferedReader(new FileReader(messageFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
			String line;
			while ((line = br.readLine()) != null) {
				Message curMessage = Message.createMessageFromRecord(line.trim());
				if (curMessage.getContent().equals(message.getContent())) {
					continue;
				}
				bw.write(line + System.getProperty("line.separator"));
			}
			bw.close();
			br.close();
			messageFile.delete();
			tempFile.renameTo(messageFile);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			unlock(lock);
		}
		return true;
	}

	/**
	 * Delete function without queue argument, this will delete message from
	 * default queue if it exists
	 * 
	 * @param message
	 *            the message
	 * @return true if deletion is successful, false otherwise
	 */
	public boolean delete(Message message) {
		return delete(DEFAULT_QUEUE, message);
	}

	// --------------------------------------------------------------------------------------
	// Queue related method

	/**
	 * Create a queue directory with specified name if any part of this function
	 * fail, previously created file/directory will be deleted
	 * 
	 * @param queue
	 *            the queue name
	 * @return true if directory is successfully created, false otherwise
	 */
	public boolean createQueue(String queue) {
		if (!isQueueExist(queue)) {
			if (new File(QUEUE_DIRECTORY + "/" + queue).mkdirs()) {
				try {
					if (createMessageFile(queue))
						return true;
					else {
						removeQueue(queue);
						return false;
					}
				} catch (IOException e) {
					e.printStackTrace();
					removeQueue(queue);
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Remove a queue directory with specified name, DEFAULT queue cannot be
	 * removed this way
	 * 
	 * @param queue
	 *            the queue directory name
	 * @return true if successfully removed, false otherwise
	 */
	public boolean removeQueue(String queue) {
		// Cannot remove DEFAULT queue
		if (queue.equals(DEFAULT_QUEUE))
			return false;

		// Target must exist and cannot be non-directory
		if (isQueueExist(queue)) {
			// Get the lock for the directory first
			File lock = getQueueLock(queue);
			try {
				lock(lock);
				// Delete all the files in the directory then the directory
				// itself
				File f = getQueue(queue);
				if (f.isDirectory()) {
					for (File c : f.listFiles())
						if (!c.delete())
							return false;
				}
				if (!f.delete()) {
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				unlock(lock);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if given queue directory with given name already exist
	 * 
	 * @param queue
	 *            the queue name
	 * @return true if exist and is directory false otherwise
	 */
	public boolean isQueueExist(String queue) {
		File file = new File(QUEUE_DIRECTORY + "/" + queue);
		return (file.exists() && file.isDirectory());
	}

	/**
	 * Get a queue directory with specific name
	 * 
	 * @param queue
	 *            the directory name
	 * @return the directory file if it exist, null otherwise
	 */
	private File getQueue(String queue) {
		File file = new File(QUEUE_DIRECTORY + "/" + queue);
		if (file.exists() && file.isDirectory()) {
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Return the default queue directory name
	 * 
	 * @return the directory names
	 */
	public String getDefaultQueueName() {
		return DEFAULT_QUEUE;
	}

	private boolean createMessageFile(String queue) throws IOException {
		File file = new File(QUEUE_DIRECTORY + "/" + queue + "/" + MESSAGE_FILE);
		if (!file.exists() || file.isDirectory()) {
			return file.createNewFile();
		} else {
			return false;
		}
	}

	/**
	 * Get the message file from a queue directory File should be exist by
	 * default the directory is created
	 * 
	 * @param queue
	 *            the name of the queue directory
	 * @return the message file if exist, null otherwise
	 */
	private File getMessageFile(String queue) {
		File file = new File(QUEUE_DIRECTORY + "/" + queue + "/" + MESSAGE_FILE);
		if (file.exists() && file.isFile()) {
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Creating temporary message file to replace original one in pull() and
	 * delete()
	 * 
	 * @param queue
	 *            the queue name
	 * @return the temporary message file
	 * @throws IOException
	 */
	private File createTemporaryMessageFile(String queue) throws IOException {
		File file = new File(QUEUE_DIRECTORY + "/" + queue + "/" + MESSAGE_FILE + "_temp");
		file.createNewFile();
		return file;
	}

	// --------------------------------------------------------------------------------------
	// Lock related method

	/**
	 * Get the lock of a queue directory
	 * 
	 * @param queue
	 *            the queue directory name
	 * @return the lock file
	 */
	private File getQueueLock(String queue) {
		File lock = new File(QUEUE_DIRECTORY + "/" + queue + "/" + LOCK_FILE);
		return lock;
	}

	/**
	 * Lock a queue directory
	 * 
	 * @param lock
	 *            the lock file
	 * @throws InterruptedException
	 */
	private void lock(File lock) throws InterruptedException {
		while (!lock.mkdir()) {
			Thread.sleep(50);
		}
	}

	/**
	 * Unlock a queue directory
	 * 
	 * @param lock
	 *            the lock file
	 */
	private void unlock(File lock) {
		lock.delete();
	}
}
