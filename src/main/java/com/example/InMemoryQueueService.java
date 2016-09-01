package com.example;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * In memory implementation of message queue service. Using timer schedule to
 * keep in track the visibility period of a message
 * 
 * @author Rudi Purnomo
 *
 */
public class InMemoryQueueService implements QueueService {
	// Default queue
	private static Deque<Message> DEFAULT_QUEUE = new LinkedBlockingDeque<Message>();
	// Map of queue (name as key)
	private static Map<String, Deque<Message>> QUEUE_MAP = new HashMap<String, Deque<Message>>();
	// Time given before pulled message that is not deleted becomes visible
	// again in the queue
	private static final long DEFAULT_VISIBILITY_TIMEOUT = 3000;

	// Instance variable
	// All messages that have been received but not deleted, mapped to its
	// respective queue
	private Map<Message, String> receivedMessages;

	// --------------------------------------------------------------------------------------
	// Method

	public InMemoryQueueService() {
		receivedMessages = new HashMap<Message, String>();
	}

	@Override
	public boolean push(String queue, Message message) {
		if (!queue.isEmpty()) {
			Deque<Message> queueD = getQueue(queue);
			return (queueD != null) ? queueD.add(message) : false;
		} else
			return DEFAULT_QUEUE.add(message);
	}

	/**
	 * Push method on default queue
	 * 
	 * @param message
	 *            the message
	 * @return true if successfully pushed, false otherwise
	 */
	public boolean push(Message message) {
		return push("", message);
	}

	@Override
	public Message pull(String queue) {
		Deque<Message> queueD = getQueue(queue);
		if (queueD == null)
			return null;
		for (Message message : queueD) {
			synchronized (message) {
				if (message.getVisiblility()) {
					message.setInvisible();
					receivedMessages.put(message, queue);
					new java.util.Timer().schedule(new java.util.TimerTask() {
						@Override
						public void run() {
							sendBack(message);
						}
					}, DEFAULT_VISIBILITY_TIMEOUT);
					return message;
				}
			}
		}
		return null;
	}

	/**
	 * Pull method on default queue
	 * 
	 * @return false if queue is empty or all messages are invisible, true
	 *         otherwise
	 */
	public Message pull() {
		return pull("");
	}

	@Override
	public boolean delete(String queue, Message message) {
		Deque<Message> queueD = getQueue(queue);
		if (queueD == null)
			return false;
		if (!queue.isEmpty())
			queueD = QUEUE_MAP.get(queue);
		else
			queueD = DEFAULT_QUEUE;
		for (Message curMessage : queueD) {
			synchronized (curMessage) {
				if (message.equals(curMessage)) {
					queueD.remove(message);
					receivedMessages.remove(message);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Delete one of the received message from default queue
	 * 
	 * @param message
	 *            the message to be deleted
	 * @return true if successfully deleted, false otherwise
	 */
	public boolean delete(Message message) {
		return delete("", message);
	}

	/**
	 * Will be executed after certain time window. Convert the pulled message to
	 * be visible at the head of the queue if it has not been deleted yet Not
	 * synchronized, as no other thread supposes to get a hold of the message
	 * TODO: Fix this, this method access modifier should be private, but it is
	 * required in the testing to be public
	 * 
	 * @return true if message is successfully set to visible, false otherwise
	 */
	public boolean sendBack(Message message) {
		synchronized (message) {
			if (receivedMessages.containsKey(message) && !message.getVisiblility()) {
				message.setVisible();
				return true;
			} else {
				return false;
			}
		}
	}

	// --------------------------------------------------------------------------------------
	// Queue related function

	/**
	 * Create a new queue and its respective lock, putting them in the map no
	 * name queue is reserved for DEFAULT_QUEUE
	 * 
	 * @return the new queue
	 */
	public Deque<Message> createQueue(String queueName) {
		if (queueName.isEmpty())
			return null;
		Deque<Message> newQueue = new LinkedBlockingDeque<Message>();
		QUEUE_MAP.put(queueName, newQueue);
		return newQueue;
	}

	/**
	 * Create a new queue with specific capacity and its respective lock,
	 * putting them in the map no name queue is reserved for DEFAULT_QUEUE
	 * 
	 * @param capacity
	 *            the capacity of new queue
	 * @return the new queue
	 */
	public Deque<Message> createQueue(String queueName, int capacity) {
		if (queueName.isEmpty())
			return null;
		Deque<Message> newQueue = new LinkedBlockingDeque<Message>(capacity);
		QUEUE_MAP.put(queueName, newQueue);
		return newQueue;
	}

	/**
	 * Remove a specific queue from the map
	 * 
	 * @param queue
	 *            the queue
	 * @return true if queue is found and removed from the map
	 */
	public boolean removeQueue(String queue) {
		return QUEUE_MAP.remove(queue) != null;
	}

	/**
	 * Get a queue from the map based on the name, empty string will return
	 * default queue
	 * 
	 * @param queue
	 *            the queue name
	 * @return
	 */
	public Deque<Message> getQueue(String queue) {
		if (queue.isEmpty())
			return DEFAULT_QUEUE;
		return QUEUE_MAP.get(queue);
	}

	/**
	 * @return the default queue
	 */
	public Deque<Message> getDefaultQueue() {
		return DEFAULT_QUEUE;
	}

	/**
	 * Get the queue map
	 * 
	 * @return the queue map
	 */
	public Map<String, Deque<Message>> getQueueMap() {
		return QUEUE_MAP;
	}

	/**
	 * Clear the content of a queue
	 * 
	 * @param queue
	 */
	public void clearQueue(String queue) {
		if (queue.isEmpty())
			clearDefaultQueue();
		QUEUE_MAP.get(queue).clear();
	}

	/**
	 * Clear the content of default queue
	 */
	public void clearDefaultQueue() {
		DEFAULT_QUEUE.clear();
	}

	/**
	 * Remove all queue from the map
	 */
	public void clearQueueMap() {
		QUEUE_MAP.clear();
	}

	// --------------------------------------------------------------------------------------
	// Other function

	/**
	 * @return the map of received messages
	 */
	public Map<Message, String> getReceivedMessages() {
		return receivedMessages;
	}

	/**
	 * Clear the received messages map
	 */
	public void clearReceivedMessages() {
		receivedMessages.clear();
	}
}
