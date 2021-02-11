package util;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO - WIP

//NOTE check for thread safety issues

public class Logger {

	protected boolean autoFlush = true;
	protected ConcurrentLinkedQueue<String> log;
	protected String prefix = "";
	protected File fWriter;
	protected LogLevels verbosity;
	
	protected enum LogLevels{
		INFO, WARN, ERROR, FATAL, DEBUG, DEBUG_1, DEBUG_2, DEVELOPMENT, TODO, DEPRECATED, NOTE
	}
	
	public Logger() {
		
	}
	
	public void print(String str) {
		
		
	}
	
	
	public void println(String str) {
		
		
	}	
	
	
	public void flush() {
		
	}
	
}
