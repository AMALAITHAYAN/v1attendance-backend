package com.example.attendance.dto.check;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepResult {
    private boolean ok;
    private String message;
    private Map<String, Object> details; // optional per-step info

    public StepResult() {}

    public StepResult(boolean ok, String message, Map<String, Object> details) {
        this.ok = ok;
        this.message = message;
        this.details = details;
    }

    public static StepResult ok(String message, Map<String, Object> details) {
        return new StepResult(true, message, details);
    }
    public static StepResult fail(String message, Map<String, Object> details) {
        return new StepResult(false, message, details);
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
