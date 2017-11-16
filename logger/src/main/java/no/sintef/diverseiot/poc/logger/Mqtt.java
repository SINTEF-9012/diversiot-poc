package no.sintef.diverseiot.poc.logger;

import java.io.IOException;
import java.util.Properties;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

public class Mqtt extends Handler {
	private Server broker;
	private InterceptHandler logger;
	
	public final int port;
	public final int websocketPort;
	
	public Mqtt(int port, int websocketPort) throws IOException {
		this.port = port;
		this.websocketPort = websocketPort;
		
		this.logger = new AbstractInterceptHandler() {
			public String getID() { return "diverseiot-logger"; }
			
			@Override
			public void onPublish(InterceptPublishMessage msg) {
				logData("MQTT", msg.getClientID(), msg.getTopicName(), msg.getPayload().nioBuffer());
			}
			
			@Override
			public void onConnect(InterceptConnectMessage msg) {
				// TODO Auto-generated method stub
				super.onConnect(msg);
			}
		};
	}

	public void start() throws IOException {
		broker = new Server();
		
		// Create configuration
		Properties config = new Properties();
		config.setProperty("port", Integer.toString(port));
		config.setProperty("websocket_port", Integer.toString(websocketPort));
		config.setProperty("host", "0.0.0.0");
		config.setProperty("allow_anonymous", "true");
		
		broker.startServer(new MemoryConfig(config));
		
		broker.addInterceptHandler(logger);
	}

	public void handle() throws IOException {
		// Nothing to do here...
	}

	public void stop() throws IOException {
		broker.stopServer();
	}
}
