package kmeansExcept;

@SuppressWarnings("serial")
public class ServerException extends Exception {

	
	public ServerException(String result) {
		
	}
	
	
	@Override
	public String getMessage() {
		return "Server Exception";
	}

}
