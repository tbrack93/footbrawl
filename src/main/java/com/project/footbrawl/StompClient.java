package com.project.footbrawl;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@EnableAsync
public class StompClient {

    private static String URL = "ws://fantasyfootbrawl.co.uk/messages/";
    public static void main(String[] args) throws InterruptedException, ExecutionException {
    	
    	WebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
    	List<Transport> transports = new ArrayList<>(1);
    	transports.add(new WebSocketTransport(simpleWebSocketClient));
    	SockJsClient sockJsClient = new SockJsClient(transports);
    	WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
    	stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    	
    	StompSessionHandler sessionHandler = new MyStompSessionHandler();
    	StompSession session = stompClient.connect(URL, sessionHandler).get();

        new Scanner(System.in).nextLine(); // Don't close immediately.
    }
}