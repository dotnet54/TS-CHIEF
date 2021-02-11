package core.exceptions;

public class NotSupportedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotSupportedException() {
		super("This feature is not supported.");
	}	
	
	public NotSupportedException(String string) {
		super(string);
	}	

}
