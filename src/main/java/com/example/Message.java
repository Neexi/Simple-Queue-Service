package com.example;

/**
 * Message class for message queue
 * 
 * @author Rudi Purnomo
 *
 */
public class Message {
	// Content of the message
	private String content;
	// Current visibility of this message
	private boolean visibility;

	// Starting index for the content in record
	private static final int RECORD_CONTENT_START_INDEX = 6;

	/**
	 * Create new message with specific input content By default message is
	 * created visible
	 * 
	 * @param content
	 *            the input content
	 */
	public Message(String content) {
		this.content = content;
		this.visibility = true;
	}

	public Message(String content, boolean visibility) {
		this.content = content;
		this.visibility = visibility;
	}

	/**
	 * Get the content of this message
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Get the visibility flag of this message
	 * 
	 * @return the visibility flag
	 */
	public boolean getVisiblility() {
		return visibility;
	}

	/**
	 * Set this message to be visible
	 */
	public void setVisible() {
		this.visibility = true;
	}

	/**
	 * Set this message to be invisible
	 */
	public void setInvisible() {
		this.visibility = false;
	}

	/**
	 * Create a text record containing visible message content
	 * 
	 * @return the record string
	 */
	public String createVisibleRecord() {
		return "{ V : " + Long.toString(0) + " },{ C : " + content + " }";
	}

	/**
	 * Create a text record that will be invisible until certain time
	 * 
	 * @param invisDuration
	 *            the time in milliseconds where the message will be invisible
	 * @return the record string
	 */
	public String createInvisibleRecord(long invisDuration) {
		long timeLimit = System.currentTimeMillis() + invisDuration;
		return "{ V : " + Long.toString(timeLimit) + " },{ C : " + content + " }";
	}

	/**
	 * Create a message object based on certain string record visibility of the
	 * message will be determined by the comparison between time that is written
	 * in the record and current time content of the message will copy the
	 * content that is written in the record. TODO : Add more sophisticated file
	 * content matcher to make sure that the record string is not tampered
	 * 
	 * @param record
	 *            the string record
	 * @return
	 */
	public static Message createMessageFromRecord(String record) {
		// Split only on the first occurrence of comma, this method is
		// consistent regardless of the content of the message
		String[] str = record.split(",", 2);
		long invisibleTime;
		try {
			invisibleTime = Long.parseLong(str[0].substring(RECORD_CONTENT_START_INDEX, str[0].length() - 2));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			invisibleTime = 0;
		}
		boolean visibility = invisibleTime < System.currentTimeMillis();
		String content = str[1].substring(RECORD_CONTENT_START_INDEX, str[1].length() - 2);
		return new Message(content, visibility);
	}
}
