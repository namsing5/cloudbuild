package com.ikea.eventmeshtopubsub;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

@Configuration
@EnableJms
@Component
@Service
public class SolaceListener implements JmsListenerConfigurer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SolaceListener.class);
	
	@Value("${sol.InQueues}")
	private String[] inQueues;
	
	@Value("${spring.cloud.gcp.topic-id}")
	private String topicID;
	
	@Autowired
	private PubSubTemplate pubsubTemplate;
	
	TransformPS trans=new TransformPS();

	@Override
	public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
		int iteration = 0;
		for (String inQueue : inQueues) {
			SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
	        endpoint.setId("myJmsEndpoint-" + iteration++);
	        endpoint.setDestination(inQueue.substring(0, inQueue.indexOf("/")));
	        endpoint.setConcurrency(inQueue.substring(inQueue.indexOf("/")+1));
	        endpoint.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					if (message instanceof TextMessage) {
						TextMessage textmessage=(TextMessage) message;
						try {
							JSONObject jsonmessage = new JSONObject(textmessage.getText());
							LOGGER.info("************Message Text************"+jsonmessage);
							//trans.splitMessage(jsonmessage);
							Map<String, String> headers = new HashMap<String, String>();
				    		for (Iterator<?> iterator = jsonmessage.getJSONObject("orderManagement").getJSONObject("header").keySet().iterator(); iterator.hasNext();) {
				    		    String key = (String) iterator.next();
				    		    headers.put(key, jsonmessage.getJSONObject("orderManagement").getJSONObject("header").get(key).toString());
				    		} 
							ListenableFuture<String> messageIdFuture=pubsubTemplate.publish(topicID, jsonmessage.toString(), headers);
				    		LOGGER.info("Message received from "+inQueue.substring(0, inQueue.indexOf("/"))+" successfully sent to GCP Topic "+topicID+". MessageID:"+messageIdFuture.get()+" received as acknowledgement from GCP!");
						} catch (JMSException | InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						
					} else if(message instanceof BytesMessage) {
						BytesMessage bytesmessage=(BytesMessage) message;
						byte[] data = null;
						try {
							data = new byte[(int) bytesmessage.getBodyLength()];
						} catch (JMSException e) {
							LOGGER.error("Error occured while decoding the bytes message data "+e);
						}
						try {
							bytesmessage.readBytes(data);
						} catch (JMSException e) {
							LOGGER.error("Error occured while reading the bytes message due to "+e);
						}
						String messageText = new String(data);
						try {
						JSONObject jsonmessage = new JSONObject(messageText);
						LOGGER.info("************Message Bytes************"+jsonmessage);
						//trans.splitMessage(jsonmessage);
						Map<String, String> headers = new HashMap<String, String>();
			    		for (Iterator<?> iterator = jsonmessage.getJSONObject("orderManagement").getJSONObject("header").keySet().iterator(); iterator.hasNext();) {
			    		    String key = (String) iterator.next();
			    		    headers.put(key, jsonmessage.getJSONObject("orderManagement").getJSONObject("header").get(key).toString());
			    		} 
						ListenableFuture<String> messageIdFuture=pubsubTemplate.publish(topicID, jsonmessage.toString(), headers);
			    		LOGGER.info("Message received from "+inQueue.substring(0, inQueue.indexOf("/"))+" successfully sent to GCP Topic "+topicID+". MessageID:"+messageIdFuture.get()+" received as acknowledgement from GCP!");
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					}
					else {
						LOGGER.info("Message received is neither a TextMessage nor a BytesMessage");
					}
				}       	
	        });      
	        registrar.registerEndpoint(endpoint);
	    }	
		
	}

}
