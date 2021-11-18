package com.heal.dashboard.service.exception;

public class UiServiceException extends Exception {
        private final String errorMessage;
        private Object errorObject;

        public UiServiceException(String message, Throwable cause, String errorMessage) {
            super(message, cause);
            this.errorMessage = errorMessage;
        }

        public UiServiceException(Object message, String errorMessage) {
            super(errorMessage);
            this.errorMessage = errorMessage;
            this.errorObject = message;
        }

        public UiServiceException(Throwable cause, String errorMessage) {
            super(cause);
            this.errorMessage = errorMessage;
        }

        public UiServiceException(String errorMessage) {
            super(errorMessage);
            this.errorMessage = errorMessage;
        }

        public String getSimpleMessage() {
            return "UiServiceException :: " + this.errorMessage;
        }

        public Object getErrorObject() {
            return this.errorObject;
        }
}
