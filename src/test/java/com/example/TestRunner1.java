package com.example;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner1 {
	public static void main(String[] args) {
		System.out.println("Following is the result for in memory queue tests:");
		Result result = JUnitCore.runClasses(InMemoryQueueTest.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.println(result.wasSuccessful() ? "All In Memory Queue tests passed!" : result.getFailureCount() + " tests fail");
	}
}
