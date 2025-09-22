// src/main/java/com/example/attendance/dto/CheckinResponse.java
package com.example.attendance.dto;
import java.util.Map;

public class CheckinResponse {
  public boolean ok;
  public String message;
  public Map<String, Object> checks; // e.g. WIFI/GEO/FACE/QR -> { ok: true/false, ... }

  public static CheckinResponse ok(Map<String,Object> checks) {
    var r = new CheckinResponse();
    r.ok = true; r.message = "Marked";
    r.checks = checks;
    return r;
  }
  public static CheckinResponse fail(String msg, Map<String,Object> checks) {
    var r = new CheckinResponse();
    r.ok = false; r.message = msg;
    r.checks = checks;
    return r;
  }
}
