package com.example;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

/**
 * Test for file based message queue
 * At the moment test is quite limited as it can only relies on implemented method
 * TODO : Add visibility timer test without thread.sleep()
 * TODO : Add additional helper methods for testing
 * @author Rudi Purnomo
 *
 */
public class FileQueueTest {
	private static FileQueueService fQueue = new FileQueueService();

	/**
	 * Default directory test
	 */
	@Test
	public void testDirectoryCheck() {
		// default queue directory has to exist
		assertEquals(fQueue.isQueueExist("DEFAULT"), true);
		System.out.println("  Default directory check passed!");
	}

	/**
	 * Test for queue directory creation and deletion
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testQueueCreateAndDelete() {
		String testQueue = "testQueue";
		if (!fQueue.isQueueExist(testQueue)) {
			// Should be able to create directory if it does not exist
			assertEquals(fQueue.createQueue(testQueue), true);
			// Directory should exist now
			assertEquals(fQueue.isQueueExist(testQueue), true);
		} else {
			// And vice versa
			assertEquals(fQueue.createQueue(testQueue), false);
		}
		assertEquals(fQueue.removeQueue(testQueue), true);
		// Directory should not exist now
		assertEquals(fQueue.isQueueExist(testQueue), false);
		System.out.println("  Queue creation and deletion test passed!");
	}
	
	/**
	 * Test the basic functionality of push, pull, and delete
	 */
	@Test
	public void testBasicPushPullDelete() {
		String defQ = fQueue.getDefaultQueueName();
		Message message1 = new Message("test message 1");
		Message message2 = new Message("test message 2");
		//Default queue has to exist
		assertEquals(fQueue.isQueueExist(defQ), true); 
		//Test push and pull functionality
		assertEquals(fQueue.push(message1), true); 
		//The message pulled from the file must have the same content as the pushed message
		assertEquals(fQueue.pull().getContent(), message1.getContent());
		//Invisible message should not be pullable
		assertEquals(fQueue.pull(), null);
		assertEquals(fQueue.delete(message1), true);
		//Test delete functionality
		//Push message 2 to the queue
		assertEquals(fQueue.push(message2), true); 
		assertEquals(fQueue.delete(message2), true);
		//Message 2 should not be pullable even though it has never been pulled before
		assertEquals(fQueue.pull(), null);
		System.out.println("  Basic operation test passed!");
	}
	
	/**
	 * Test if the queue has proper FIFO structure
	 */
	@Test
	public void testFIFO() {
		Message message1 = new Message("test message 1");
		Message message2 = new Message("test message 2");
		//Push both message into the queue
		assertEquals(fQueue.push(message1), true); 
		assertEquals(fQueue.push(message2), true); 
		//Message 1 should be the result of first pull
		assertEquals(fQueue.pull().getContent(), message1.getContent());
		//And then message 2
		assertEquals(fQueue.pull().getContent(), message2.getContent());
		//Delete for cleanup
		assertEquals(fQueue.delete(message1), true);
		assertEquals(fQueue.delete(message2), true);
		
		System.out.println("  Basic FIFO test passed!");
	}
	
	/**
	 * This is a visibility test that uses thread.sleep()
	 * Disable the comment to run this test
	 * TODO : Replace with test that does not use sleep
	 */
	//@Test
	public void testVisibility() {
		Message message = new Message("test message");
		assertEquals(fQueue.push(message), true); 
		assertEquals(fQueue.pull().getContent(), message.getContent());
		//It should not be pullable now
		assertEquals(fQueue.pull(), null);
		//Now wait for the message to be visible again
		
		try {
			Thread.sleep(3500);
			//Message should be pullable again after wait
			assertEquals(fQueue.pull().getContent(), message.getContent());
		} catch (InterruptedException e) {
			System.out.println("  Visibility test interrupted");
			e.printStackTrace();
			return;
		} finally {
			fQueue.delete(message);
		}
		System.out.println("  Basic visibility test passed!");
	}
}
