package com.example.attendance.service;

import com.example.attendance.dto.StudentCreateRequest;
import com.example.attendance.face.FaceService;
import com.example.attendance.face.FaceServiceException;
import com.example.attendance.model.StudentProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.util.ByteArrayMultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

@Service
public class StudentService {

    private static final String DEFAULT_TEMP_PASSWORD = "1234";

    private final UserService userService;
    private final StudentProfileRepository studentRepo;
    private final FaceService faceService;

    public StudentService(UserService userService,
                          StudentProfileRepository studentRepo,
                          FaceService faceService) {
        this.userService = userService;
        this.studentRepo = studentRepo;
        this.faceService = faceService;
    }

    // ---------------------------
    // Single create (photo as bytes)
    // ---------------------------
    @Transactional
    public StudentProfile createStudent(StudentCreateRequest req, byte[] photoBytes) {
        if (userService.exists(req.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + req.getUsername());
        }
        User u = userService.createUser(req.getUsername(), req.getPassword(), UserRole.STUDENT);

        StudentProfile s = new StudentProfile();
        s.setUser(u);
        s.setUsername(req.getUsername());            // NEW: persist username column
        s.setName(req.getName());
        s.setRollNo(req.getRollNo());
        s.setClassName(req.getClassName());
        s.setYear(req.getYear());
        s.setDepartment(req.getDepartment());
        s.setPhoto(photoBytes);

        StudentProfile saved = studentRepo.save(s);

        // Register face with Face++ if a photo was uploaded
        tryRegisterFaceWithBytes(photoBytes);

        return saved;
    }

    // ---------------------------
    // Single create (photo as MultipartFile) – convenience overload
    // ---------------------------
    @Transactional
    public StudentProfile createStudent(StudentCreateRequest req, MultipartFile photo) {
        byte[] photoBytes = toBytes(photo);
        StudentProfile saved = createStudent(req, photoBytes);

        // Prefer sending the original MultipartFile to Face++ (saves a re-encode)
        tryRegisterFaceWithMultipart(photo);

        return saved;
    }

    // ---------------------------
    // Bulk upload (.xlsx)
    // Required columns (case-insensitive):
    //   username | name | rollNo | className | year | department
    // Optional:
    //   password | photoBase64   (Base64 or data URL; if provided, will be saved & registered)
    // ---------------------------
    @Transactional
    public Map<String, Object> bulkUpload(MultipartFile file) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Empty Excel sheet");

            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Missing header row");

            Map<String, Integer> col = mapHeader(header);

            String[] required = {"username", "name", "rollno", "classname", "year", "department"};
            for (String r : required) {
                if (!col.containsKey(r)) {
                    throw new IllegalArgumentException("Missing required column: " + r);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String username = readCell(row, col.get("username"));
                if (isBlank(username)) { skipped.add("row " + (r + 1) + " (no username)"); continue; }
                if (userService.exists(username)) { skipped.add(username + " (exists)"); continue; }

                String name       = readCell(row, col.get("name"));
                String rollNo     = readCell(row, col.get("rollno"));
                String className  = readCell(row, col.get("classname"));
                String year       = readCell(row, col.get("year"));
                String department = readCell(row, col.get("department"));
                String password   = col.containsKey("password") ? readCell(row, col.get("password")) : DEFAULT_TEMP_PASSWORD;

                byte[] photoBytes = null;
                if (col.containsKey("photobase64")) {
                    String b64 = readCell(row, col.get("photobase64"));
                    photoBytes = decodeBase64Image(b64);
                }

                // create user + profile
                User u = userService.createUser(username, defaultIfBlank(password, DEFAULT_TEMP_PASSWORD), UserRole.STUDENT);

                StudentProfile s = new StudentProfile();
                s.setUser(u);
                s.setUsername(username);              // NEW: persist username column
                s.setName(name);
                s.setRollNo(rollNo);
                s.setClassName(className);
                s.setYear(year);
                s.setDepartment(department);
                s.setPhoto(photoBytes);

                studentRepo.save(s);
                created.add(username);

                // Register face if provided
                if (photoBytes != null && photoBytes.length > 0) {
                    tryRegisterFaceWithBytes(photoBytes);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bulk upload failed: " + e.getMessage(), e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("createdCount", created.size());
        res.put("createdUsers", created);
        res.put("skippedCount", skipped.size());
        res.put("skipped", skipped);
        return res;
    }

    // =========================
    // Helpers
    // =========================

    private void tryRegisterFaceWithBytes(byte[] photoBytes) {
        try {
            if (photoBytes != null && photoBytes.length > 0) {
                ByteArrayMultipartFile mf =
                        new ByteArrayMultipartFile(photoBytes, "photo", "photo.jpg", "image/jpeg");
                faceService.processFace(mf, "register");
            }
        } catch (FaceServiceException | IOException | RuntimeException ignored) {
            // Don’t fail core flow on Face++ errors
        }
    }

    private void tryRegisterFaceWithMultipart(MultipartFile photo) {
        try {
            if (photo != null && !photo.isEmpty()) {
                faceService.processFace(photo, "register");
            }
        } catch (FaceServiceException | IOException | RuntimeException ignored) {
            // Don’t fail core flow on Face++ errors
        }
    }

    private byte[] toBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            return file.getBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, Integer> mapHeader(Row header) {
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            c.setCellType(CellType.STRING);
            String key = c.getStringCellValue() == null ? "" : c.getStringCellValue().trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty()) col.put(key, i);
        }
        return col;
    }

    private String readCell(Row row, Integer idx) {
        if (idx == null || idx < 0) return null;
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellType(CellType.STRING);
        String v = cell.getStringCellValue();
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String defaultIfBlank(String s, String def) { return isBlank(s) ? def : s.trim(); }

    /**
     * Accepts raw base64 or a data URL (e.g., "data:image/jpeg;base64,<...>")
     */
    private byte[] decodeBase64Image(String maybeDataUrl) {
        if (isBlank(maybeDataUrl)) return null;
        String s = maybeDataUrl.trim();
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma > 0) {
            s = s.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
