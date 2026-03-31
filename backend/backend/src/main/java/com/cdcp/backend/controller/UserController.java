package com.cdcp.backend.controller;

import com.cdcp.backend.entity.User;
import com.cdcp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        int lastDash = token.lastIndexOf("-");
        if (lastDash == -1) return null;
        try {
            Long userId = Long.parseLong(token.substring(lastDash + 1));
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<?> uploadResume(@RequestHeader("Authorization") String authHeader, @RequestParam("file") MultipartFile file) {
        User user = getUserFromToken(authHeader);
        if (user == null || !"student".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Only students can upload a resume"));
        }

        try {
            user.setResume(file.getBytes());
            user.setResumeFilename(file.getOriginalFilename());
            userRepository.save(user);
            return ResponseEntity.ok(new ErrorResponse("Resume uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to upload resume"));
        }
    }

    @GetMapping("/resume/{studentId}")
    public ResponseEntity<byte[]> getResume(@PathVariable Long studentId) {
        Optional<User> userOpt = userRepository.findById(studentId);
        
        if (!userOpt.isPresent() || userOpt.get().getResume() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User user = userOpt.get();
        if (!"student".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + user.getResumeFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(user.getResume());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }
        // Return profile without password or binary resume blob
        java.util.Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("department", user.getDepartment());
        profile.put("cgpa", user.getCgpa());
        profile.put("backlogCount", user.getBacklogCount());
        profile.put("skills", user.getSkills());
        profile.put("hasResume", user.getResume() != null);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authHeader, @RequestBody java.util.Map<String, Object> updates) {
        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }
        if (updates.containsKey("firstName")) user.setFirstName((String) updates.get("firstName"));
        if (updates.containsKey("lastName")) user.setLastName((String) updates.get("lastName"));
        if (updates.containsKey("department")) user.setDepartment((String) updates.get("department"));
        if (updates.containsKey("cgpa") && updates.get("cgpa") != null) user.setCgpa(Double.parseDouble(updates.get("cgpa").toString()));
        if (updates.containsKey("backlogCount") && updates.get("backlogCount") != null) user.setBacklogCount(Integer.parseInt(updates.get("backlogCount").toString()));
        if (updates.containsKey("skills")) user.setSkills((String) updates.get("skills"));
        userRepository.save(user);
        return ResponseEntity.ok(new ErrorResponse("Profile updated successfully"));
    }
}
