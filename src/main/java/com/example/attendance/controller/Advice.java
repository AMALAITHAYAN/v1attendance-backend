package com.example.attendance.controller;

import com.example.attendance.service.CheckFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class Advice {
  @ExceptionHandler(CheckFailedException.class)
  public ResponseEntity<?> onCheckFailed(CheckFailedException ex) {
    Map<String,Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", ex.getMessage());
    body.put("checks", ex.getChecks());
    return ResponseEntity.status(409).body(body);
  }
}
