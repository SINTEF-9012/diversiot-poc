package no.sintef.diverseiot.poc.logger;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

public abstract class Handler {
	public abstract void start() throws IOException;
	public abstract void handle() throws IOException;
	public abstract void stop() throws IOException;
	
	// Logging
	private static final Object logLock = new Object();
	private static PrintStream logOutput = System.out;
	
	public static void setLogOutput(PrintStream log) {
		synchronized (logLock) {
			logOutput = log;
		}
	}
	
	protected void logData(String type, String src, String target, ByteBuffer data) {
		synchronized (logLock) {
			if (logOutput != null) {
				//Write to the log-output
				logOutput.print(type);
				logOutput.print(" | ");
				logOutput.print(src);
				logOutput.print(" | ");
				logOutput.print(target);
				logOutput.print(" | ");
				boolean first = true;
				data.rewind();
				while (data.hasRemaining()) {
					if (first) first = false;
					else logOutput.print(",");
					
					byte b = data.get();
					logOutput.format("0x%02X", b);
				}
				logOutput.println();
			}
		}
	}
}
