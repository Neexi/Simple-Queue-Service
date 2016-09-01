package com.example;

import com.amazonaws.services.sqs.AmazonSQSClient;

public class SqsQueueService implements QueueService {
	//
	// Task 4: Optionally implement parts of me.
	//
	// This file is a placeholder for an AWS-backed implementation of
	// QueueService. It is included
	// primarily so you can quickly assess your choices for method signatures in
	// QueueService in
	// terms of how well they map to the implementation intended for a
	// production environment.
	//

	public SqsQueueService(AmazonSQSClient sqsClient) {
	}

	@Override
	public boolean push(String queue, Message message) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Message pull(String queue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(String queue, Message message) {
		// TODO Auto-generated method stub
		return false;
	}
}
