package com.mobiquity.mydropbox.event;

public class OnUploadFailedEvent {

    private Exception exception;

    public OnUploadFailedEvent(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
