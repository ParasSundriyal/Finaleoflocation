package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/soochna")
public class SoochnaController {

    @GetMapping("/user")
    public ResponseEntity<List<SoochnaItem>> getUserSoochnaItems() {
        try {
            // TODO: Replace with actual database implementation
            List<SoochnaItem> dummyData = new ArrayList<>();
            
            LocalDateTime now = LocalDateTime.now();
            
            // Add some dummy data for testing
            dummyData.add(new SoochnaItem(
                "1",
                "पानी की समस्या",
                "गांव में पानी की आपूर्ति में समस्या आ रही है",
                "submitted",
                now.minusHours(2).format(DateTimeFormatter.ISO_DATE_TIME)
            ));
            
            dummyData.add(new SoochnaItem(
                "2",
                "सड़क मरम्मत",
                "मुख्य सड़क की मरम्मत की आवश्यकता है",
                "pending",
                now.minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME)
            ));
            
            dummyData.add(new SoochnaItem(
                "3",
                "बिजली की समस्या",
                "बिजली की आपूर्ति में अनियमितता",
                "verified",
                now.format(DateTimeFormatter.ISO_DATE_TIME)
            ));
            
            return ResponseEntity.ok(dummyData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

class SoochnaItem {
    private String id;
    private String title;
    private String description;
    private String status;
    private String timestamp;

    public SoochnaItem(String id, String title, String description, String status, String timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
} 