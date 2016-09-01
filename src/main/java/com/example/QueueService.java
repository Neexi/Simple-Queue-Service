package com.example;

/**
 * Interface for Message Queue service
 * 
 * @author Rudi Purnomo
 *
 */
public interface QueueService {

	/**
	 * Push a message to the end of queue
	 * 
	 * @param queue
	 *            the specific queue name
	 * @param message
	 *            the message
	 * 
	 * @return true if successfully pushed, false otherwise
	 */
	public boolean push(String queue, Message message);

	/**
	 * Get the first visible message from the queue and set it invisible. After
	 * certain period, the message will be set to visible again if not deleted
	 * 
	 * @param queue
	 *            the specific queue name
	 * @return null if queue is empty or all messages are invisible, the first
	 *         visible message otherwise
	 */
	public Message pull(String queue);

	/**
	 * Delete a message from its respective queue
	 * 
	 * @param queue
	 *            the specified queue name
	 * @param message
	 *            the message
	 * 
	 * @return true if successfully deleted, false otherwise
	 */
	public boolean delete(String queue, Message message);

}
