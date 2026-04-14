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

    @GetMapping("/reports/department")
    public ResponseEntity<?> getDepartmentReports(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));
        
        List<User> students = userRepository.findAll().stream().filter(u -> "student".equals(u.getRole())).collect(Collectors.toList());
        List<Application> apps = applicationRepository.findAll();
        
        Map<String, Map<String, Object>> deptStats = new HashMap<>();
        
        for (User student : students) {
            String dept = student.getDepartment();
            if (dept == null || dept.trim().isEmpty()) dept = "Unknown";
            
            deptStats.putIfAbsent(dept, new HashMap<>(Map.of("totalStudents", 0, "placedStudents", 0)));
            Map<String, Object> stats = deptStats.get(dept);
            stats.put("totalStudents", (int)stats.get("totalStudents") + 1);
            
            boolean isPlaced = apps.stream().anyMatch(a -> a.getStudentId().equals(student.getId()) && ("SELECTED".equals(a.getStatus()) || "ACCEPTED".equals(a.getStatus())));
            if (isPlaced) {
                stats.put("placedStudents", (int)stats.get("placedStudents") + 1);
            }
        }
        
        for (Map.Entry<String, Map<String, Object>> entry : deptStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            int total = (int)stats.get("totalStudents");
            int placed = (int)stats.get("placedStudents");
            double percentage = total == 0 ? 0.0 : ((double)placed / total) * 100.0;
            stats.put("placementPercentage", percentage);
            stats.put("department", entry.getKey());
        }
        
        return ResponseEntity.ok(deptStats.values());
    }

    @GetMapping("/reports/student")
    public ResponseEntity<?> getStudentReports(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Admin only"));
        
        List<User> students = userRepository.findAll().stream().filter(u -> "student".equals(u.getRole())).collect(Collectors.toList());
        List<Application> apps = applicationRepository.findAll();
        
        List<Map<String, Object>> studentStats = students.stream().map(student -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("studentId", student.getId());
            stats.put("name", student.getFirstName() + " " + student.getLastName());
            stats.put("department", student.getDepartment());
            
            List<Application> studentApps = apps.stream().filter(a -> a.getStudentId().equals(student.getId())).collect(Collectors.toList());
            stats.put("appsCount", studentApps.size());
            
            boolean isPlaced = studentApps.stream().anyMatch(a -> "SELECTED".equals(a.getStatus()) || "ACCEPTED".equals(a.getStatus()));
            stats.put("status", isPlaced ? "Placed" : "Unplaced");
            
            return stats;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(studentStats);
    }
}
