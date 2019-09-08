package com.project.footbrawl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import com.project.footbrawl.instance.MessageFromClient;
import com.project.footbrawl.instance.MessageToClient;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@EnableAsync
public class MyStompSessionHandler extends StompSessionHandlerAdapter {
	
	private static Calendar now = Calendar.getInstance();
	private static Date time = new Date(now.getTimeInMillis() + 100000);

    private Logger logger = LogManager.getLogger(MyStompSessionHandler.class);
    

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
    	Timer timer = new Timer();
        logger.info("New session established : " + session.getSessionId());
        session.subscribe("/topic/game/744", this);
        session.subscribe("/queue/game/744/1", this); 
        TimerTask t = new TimerTask() {
        	public void run() {
        		for(int i = 0; i < 100; i++) {
        		  logger.info("sending");
        		   session.send("/app/game/gameplay/744/1", getMovementRequest());
        		}
        	}
        };
       timer.schedule(t, time);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return MessageToClient.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        logger.info("Received");
    }
    
    private MessageFromClient getMovementRequest() {
    	MessageFromClient m = new MessageFromClient();
    	m.setType("INFO");
    	m.setAction("MOVEMENT");
    	m.setPlayer(6);
    	m.setLocation(new int[] {13,10});
    	m.setRouteMACost(0);
    	return m;
    }
}