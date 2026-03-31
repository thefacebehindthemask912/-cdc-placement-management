package com.cdcp.backend.controller;

import com.cdcp.backend.entity.Application;
import com.cdcp.backend.entity.Job;
import com.cdcp.backend.entity.User;
import com.cdcp.backend.repository.ApplicationRepository;
import com.cdcp.backend.repository.JobRepository;
import com.cdcp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;

    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        int lastDash = token.lastIndexOf("-");
        if (lastDash == -1) return null;
        try {
            Long userId = Long.parseLong(token.substring(lastDash + 1));
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) { return null; }
    }

    private boolean isAdmin(String authHeader) {
        User u = getUserFromToken(authHeader);
        return u != null && "admin".equals(u.getRole());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));

        List<User> allUsers = userRepository.findAll();
        List<Job> allJobs = jobRepository.findAll();
        List<Application> allApps = applicationRepository.findAll();

        long students = allUsers.stream().filter(u -> "student".equals(u.getRole())).count();
        long companies = allUsers.stream().filter(u -> "company".equals(u.getRole())).count();
        long accepted = allApps.stream().filter(a -> "ACCEPTED".equals(a.getStatus()) || "SELECTED".equals(a.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", students);
        stats.put("totalCompanies", companies);
        stats.put("totalJobs", allJobs.size());
        stats.put("totalApplications", allApps.size());
        stats.put("totalPlacements", accepted);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));

        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId()); m.put("email", u.getEmail()); m.put("role", u.getRole());
            m.put("firstName", u.getFirstName()); m.put("lastName", u.getLastName());
            m.put("department", u.getDepartment()); m.put("cgpa", u.getCgpa());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));
        if (!userRepository.existsById(id)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        userRepository.deleteById(id);
        return ResponseEntity.ok(new ErrorResponse("User deleted"));
    }

    @GetMapping("/applications")
    public ResponseEntity<?> getAllApplications(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));
        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @GetMapping("/company-stats")
    public ResponseEntity<?> getCompanyStats(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));

        List<Application> apps = applicationRepository.findAll();
        Map<String, Long> companyWise = apps.stream()
                .collect(Collectors.groupingBy(a -> a.getCompany() != null ? a.getCompany() : "Unknown", Collectors.counting()));

        return ResponseEntity.ok(companyWise);
    }
}
