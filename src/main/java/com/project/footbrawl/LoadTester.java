package com.project.footbrawl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
public class LoadTester {
	
	public static boolean run = false;

	public static void main(String[] args) {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
		for(int i = 0 ; i <100 ; i++) {
			Thread thread = new Thread() {
				public void run() {
					System.out.println("running"); 
					try {
						StompClient.main(args);
					} catch (InterruptedException | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			executor.submit(thread);
			System.out.println("thread " + i + " started");
		}
	}
	
}
