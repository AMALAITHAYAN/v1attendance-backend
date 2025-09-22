package com.example.attendance.face;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.util.Arrays;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FaceService {

    // You had these as constants; I made them externalizable (same defaults).
    @Value("${face.api.key:s17AGO3iWNfbdI9QkkyIiS6daUW8lk0K}")
    private String API_KEY;
    @Value("${face.api.secret:gZMIiSh3YwJvU3d29-SSU-Uw0KdPiXSV}")
    private String API_SECRET;
    @Value("${face.faceset.outerId:employee_faces}")
    private String FACESET_OUTER_ID;

    private static final int MIN_SIZE = 64, MAX_SIZE = 1920;

    public Map<String, Object> processFace(MultipartFile file, String action)
            throws FaceServiceException, IOException {
        if (!List.of("register", "checkin").contains(action)) {
            throw new FaceServiceException("Invalid action", 400);
        }

        File temp = File.createTempFile("face", ".jpg");
        try (InputStream in = file.getInputStream();
             FileOutputStream out = new FileOutputStream(temp)) {
            in.transferTo(out);
            resizeImageIfNeeded(temp);
            String faceToken = detectFace(temp);
            if (faceToken == null) {
                throw new FaceServiceException("No face detected in the image.", 400);
            }

            if (action.equals("register")) {
                boolean created = addFaceToFaceset(faceToken);
                if (created)
                    return Map.of("success", true, "message", "Face registered successfully.");
                throw new FaceServiceException("Failed to add face to FaceSet.", 500);
            } else {
                boolean matched = searchFace(faceToken);
                if (matched)
                    return Map.of("success", true, "message", "Check-in successful. Face recognized.");
                throw new FaceServiceException("Face not recognized. Check-in denied.", 401);
            }
        } catch (IOException ex) {
            throw new FaceServiceException("File processing error.", 500);
        } finally {
            temp.delete();
        }
    }

    private void resizeImageIfNeeded(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        int width = img.getWidth(), height = img.getHeight();
        double scale = 1.0;
        if (width < MIN_SIZE) scale = Math.max(scale, ((double) MIN_SIZE) / width);
        if (height < MIN_SIZE) scale = Math.max(scale, ((double) MIN_SIZE) / height);
        if (width > MAX_SIZE) scale = Math.min(scale, ((double) MAX_SIZE) / width);
        if (height > MAX_SIZE) scale = Math.min(scale, ((double) MAX_SIZE) / height);
        int newWidth = (int)(width * scale), newHeight = (int)(height * scale);
        if (scale != 1.0) {
            BufferedImage resized = new BufferedImage(newWidth, newHeight, img.getType());
            Graphics2D g = resized.createGraphics();
            g.drawImage(img, 0, 0, newWidth, newHeight, null);
            g.dispose();
            ImageIO.write(resized, "jpg", file);
        }
    }

    private String detectFace(File imageFile) throws IOException {
        String url = "https://api-us.faceplusplus.com/facepp/v3/detect";
        HttpPost post = new HttpPost(url);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("image_file", imageFile);
        builder.addTextBody("api_key", API_KEY);
        builder.addTextBody("api_secret", API_SECRET);
        post.setEntity(builder.build());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonNode node = new ObjectMapper().readTree(json);
            if (node.has("faces") && node.get("faces").size() > 0)
                return node.get("faces").get(0).get("face_token").asText();
            return null;
        }
    }

    private boolean addFaceToFaceset(String faceToken) throws IOException {
        String url = "https://api-us.faceplusplus.com/facepp/v3/faceset/addface";
        HttpPost post = new HttpPost(url);

        List<NameValuePair> params = Arrays.asList(
                new BasicNameValuePair("api_key", API_KEY),
                new BasicNameValuePair("api_secret", API_SECRET),
                new BasicNameValuePair("outer_id", FACESET_OUTER_ID),
                new BasicNameValuePair("face_tokens", faceToken)
        );
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonNode node = new ObjectMapper().readTree(json);
            return node.path("face_added").asInt(0) > 0;
        }
    }

    private boolean searchFace(String faceToken) throws IOException {
        String url = "https://api-us.faceplusplus.com/facepp/v3/search";
        HttpPost post = new HttpPost(url);

        List<NameValuePair> params = Arrays.asList(
                new BasicNameValuePair("api_key", API_KEY),
                new BasicNameValuePair("api_secret", API_SECRET),
                new BasicNameValuePair("face_token", faceToken),
                new BasicNameValuePair("outer_id", FACESET_OUTER_ID)
        );
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonNode node = new ObjectMapper().readTree(json);
            JsonNode results = node.path("results");
            if (results.size() > 0) {
                double confidence = results.get(0).path("confidence").asDouble(0);
                return confidence > 80.0; // threshold
            }
            return false;
        }
    }
}
