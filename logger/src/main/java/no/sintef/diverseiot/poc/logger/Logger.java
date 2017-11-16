package no.sintef.diverseiot.poc.logger;

import java.io.IOException;
import java.io.PrintStream;

public class Logger 
{
    public static void main( String[] args ) throws IOException, InterruptedException
    {
    	// Add all the handlers
    	Handler[] handlers = {
    		new TcpPairs(5000, 100, "SERIAL"),
    		new Mqtt(1883,8080),
    	};
    	
    	// Open a file to start logging
    	PrintStream output = new PrintStream("/root/output.log");
    	Handler.setLogOutput(output);
    	
    	// Start all the handlers
    	for (Handler handler : handlers) 
    		handler.start();
    	
    	// Handle everything forever
    	while(true) {
    		for (Handler handler : handlers) {
    			handler.handle();
    		}
    		// Sleep a bit to release resources
    		Thread.sleep(100);
    	}
    	
    	
    }
}