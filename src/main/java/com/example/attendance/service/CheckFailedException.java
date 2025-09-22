package com.example.attendance.service;

import java.util.Map;

public class CheckFailedException extends RuntimeException {
  private final Map<String, Object> checks;

  public CheckFailedException(String message, Map<String, Object> checks) {
    super(message);
    this.checks = checks;
  }

  public Map<String, Object> getChecks() { return checks; }
}
