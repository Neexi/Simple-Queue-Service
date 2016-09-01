package com.example;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner2 {
	public static void main(String[] args) {
		System.out.println("Following is the result for file based queue tests:");
		Result result = JUnitCore.runClasses(FileQueueTest.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.println(result.wasSuccessful() ? "All File Queue tests passed!" : result.getFailureCount() + " tests fail");
	}
}
