package com.example;

import static org.junit.Assert.assertEquals;

import java.util.Deque;

import org.junit.Test;

/**
 * Test for in memory message queue
 * @author Rudi Purnomo
 *
 */
public class InMemoryQueueTest {
	private static InMemoryQueueService imQueue = new InMemoryQueueService();

	/**
	 * Test initial state of the data structure used
	 */
	@Test
	public void testInitialState() {
		preTestCleanUp();
		//Default queue should exist
		assertEquals(imQueue.getDefaultQueue() != null, true);
		// Default queue should be empty by default
		assertEquals(imQueue.getDefaultQueue().size(), 0);
		// No message should be received yet
		assertEquals(imQueue.getReceivedMessages().size(), 0);
		//Queue map should exist
		assertEquals(imQueue.getQueueMap() != null, true);
		//And be empty
		assertEquals(imQueue.getQueueMap().size(), 0);
		System.out.println("  Initial state test passed!");
	}

	/**
	 * Test if a message can be properly 
	 * 1.pushed into queue
	 * 2.pulled from queue 
	 * 3.deleted from queue
	 */
	@Test
	public void testBasicPushPullDelete() {
		preTestCleanUp();
		// Push test
		Message message = new Message("message");
		imQueue.push(message);
		// There should be 1 message in the queue now
		assertEquals(imQueue.getDefaultQueue().size(), 1);
		// Nothing should be received at this point
		assertEquals(imQueue.getReceivedMessages().size(), 0);

		// Pull test
		Message received = imQueue.pull();
		// There should still be 1 message in the queue now, message won't be
		// deleted from queue after pulled
		assertEquals(imQueue.getDefaultQueue().size(), 1);
		// There should be 1 received message
		assertEquals(imQueue.getReceivedMessages().size(), 1);
		// Received message should be the same one with the one sent before
		assertEquals(message, received);

		// Delete test
		imQueue.delete(message);
		// Queue should be empty now
		assertEquals(imQueue.getDefaultQueue().size(), 0);
		// Received message should be empty as well
		assertEquals(imQueue.getReceivedMessages().size(), 0);

		System.out.println("  Basic push, pull and delete test on default queue passed!");
	}

	/**
	 * Test if the queue has proper FIFO structure
	 */
	@Test
	public void testFIFO() {
		preTestCleanUp();
		Message message1 = new Message("message1");
		Message message2 = new Message("message2");
		imQueue.push(message1);
		imQueue.push(message2);
		Message received1 = imQueue.pull();
		// Message 1 should be received
		assertEquals(message1, received1);
		Message received2 = imQueue.pull();
		// Now message 2 should be received
		assertEquals(message2, received2);
		// There should be 2 different messages received at this point
		assertEquals(imQueue.getReceivedMessages().size(), 2);

		System.out.println("  Basic FIFO test passed!");
	}

	/**
	 * Test if the pulled message will eventually be visible again in the queue
	 * and that a deleted message will not be visible again
	 */
	@Test
	public void testVisibility() {
		preTestCleanUp();
		Message message = new Message("message");
		imQueue.push(message);
		// Pull the message, it should be invisible in the queue now
		imQueue.pull();
		assertEquals(imQueue.getReceivedMessages().size(), 1);
		Message received2 = imQueue.pull();
		// Nothing should be received even though the queue is not empty, as the
		// message is invisible by this point
		assertEquals(imQueue.getDefaultQueue().size() == 0, false);
		assertEquals(received2, null);

		// Execute the sendback function, the message should be pullable again
		// from the queue
		imQueue.sendBack(message);
		received2 = imQueue.pull();
		assertEquals(message, received2);
		// Duplicate message should not be added to map
		assertEquals(imQueue.getReceivedMessages().size(), 1);

		// Now delete the message before trying to do the sendback
		imQueue.delete(message);
		imQueue.sendBack(message);
		Message received3 = imQueue.pull();
		// Nothing should be received as the message is already deleted from
		// the queue
		assertEquals(received3, null);
		
		System.out.println("  Basic visibility test passed!");
	}
	
	/**
	 * Testing the custom queue functionality
	 */
	@Test
	public void testQueue() {
		preTestCleanUp();
		String customQueueString = "customQueue";
		Deque<Message> customQueue = imQueue.createQueue(customQueueString);
		//Making sure that the newly created custom queue can be retrieved from the service
		assertEquals(imQueue.getQueue(customQueueString), customQueue);
		
		//Testing push, pull, and delete, copy and pasted from testBasicPushPullDelete
		// Push test
		Message message = new Message("message");
		imQueue.push(customQueueString, message);
		// There should be 1 message in the queue now
		assertEquals(imQueue.getQueue(customQueueString).size(), 1);
		// Nothing should be received at this point
		assertEquals(imQueue.getReceivedMessages().size(), 0);

		// Pull test
		Message received = imQueue.pull(customQueueString);
		// There should still be 1 message in the queue now, message won't be
		// deleted from queue after pulled
		assertEquals(imQueue.getQueue(customQueueString).size(), 1);
		// There should be 1 received message
		assertEquals(imQueue.getReceivedMessages().size(), 1);
		// Received message should be the same one with the one sent before
		assertEquals(message, received);

		// Delete test
		imQueue.delete(customQueueString, message);
		// Queue should be empty now
		assertEquals(imQueue.getQueue(customQueueString).size(), 0);
		// Received message should be empty as well
		assertEquals(imQueue.getReceivedMessages().size(), 0);
		
		//Push the message back for additional testing
		imQueue.push(customQueueString, message);
		
		//Now delete the queue from the service, making sure that the deletion is successful
		assertEquals(imQueue.removeQueue(customQueueString), true);
		//Should not be able to push into the queue now
		Message message2 = new Message("message2");
		assertEquals(imQueue.push(customQueueString, message2), false);
		//Neither is pull nor delete
		assertEquals(imQueue.pull(customQueueString), null);
		assertEquals(imQueue.delete(customQueueString, message), false);
		
		System.out.println("  Basic operation test on custom queue passed!");
	}

	/**
	 * Cleaning up the queue and map, called before all tests
	 */
	private void preTestCleanUp() {
		imQueue.clearDefaultQueue();
		imQueue.clearReceivedMessages();
		imQueue.clearQueueMap();
	}
}
