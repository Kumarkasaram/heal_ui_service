package com.heal.dashboard.service.exception;



public class ClientException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String errorMessage;

    public ClientException(Throwable cause, String errorMessage) {
        super(errorMessage, cause);
        this.errorMessage = errorMessage;
    }

    public ClientException(String errorMessage)
    {
        super("ClientException : "+ errorMessage);
        this.errorMessage  = errorMessage;
    }

    public String getSimpleMessage()    {
        return "ClientException :: "+this.errorMessage;
    }

}

