package com.example.attendance.face;

public class FaceServiceException extends Exception {
    private final int status;
    public FaceServiceException(String msg, int status) {
        super(msg);
        this.status = status;
    }
    public int getStatus() { return status; }
}
