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

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/applications")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

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

    @GetMapping
    public ResponseEntity<?> getApplications(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Long jobId) {
        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }

        if ("student".equals(user.getRole())) {
            return ResponseEntity.ok(applicationRepository.findByStudentId(user.getId()));
        } else if ("company".equals(user.getRole()) && jobId != null) {
            return ResponseEntity.ok(applicationRepository.findByJobId(jobId));
        } else if ("admin".equals(user.getRole())) {
            return ResponseEntity.ok(applicationRepository.findAll());
        }

        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> applyToJob(@RequestHeader("Authorization") String authHeader, @RequestBody ApplicationRequest request) {
        User user = getUserFromToken(authHeader);
        if (user == null || !"student".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Only students can apply to jobs"));
        }

        Optional<Job> jobOpt = jobRepository.findById(request.getJobId());
        if (!jobOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Job not found"));
        }

        if (applicationRepository.existsByStudentIdAndJobId(user.getId(), request.getJobId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("You have already applied to this job"));
        }

        Job job = jobOpt.get();

        // Eligibility verification (SRS FR-J2)
        if (job.getRequiredCgpa() != null && user.getCgpa() != null && user.getCgpa() < job.getRequiredCgpa()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("You are not eligible: CGPA required is " + job.getRequiredCgpa() + " but yours is " + user.getCgpa()));
        }
        if (job.getMaxBacklogs() != null && user.getBacklogCount() != null && user.getBacklogCount() > job.getMaxBacklogs()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("You are not eligible: Max backlogs allowed is " + job.getMaxBacklogs() + " but you have " + user.getBacklogCount()));
        }

        Application application = new Application();
        application.setJobId(job.getId());
        application.setJobTitle(job.getTitle());
        application.setCompany(job.getCompany());
        application.setStudentId(user.getId());
        application.setStudentName(user.getEmail());
        application.setStudentEmail(user.getEmail());
        application.setAppliedDate(new Date());
        application.setStatus("PENDING");
        
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStatus(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody ApplicationRequest request) {
        User user = getUserFromToken(authHeader);
        if (user == null || !"company".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Only companies can update status"));
        }

        Optional<Application> appOpt = applicationRepository.findById(id);
        if (!appOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Application not found"));
        }

        Application application = appOpt.get();
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }
        if (request.getCurrentStage() != null) {
            application.setCurrentStage(request.getCurrentStage());
        }
        
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    @PostMapping("/{id}/offer")
    public ResponseEntity<?> uploadOfferLetter(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        User user = getUserFromToken(authHeader);
        if (user == null || !"company".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Only companies can upload offer letters"));
        }

        Optional<Application> appOpt = applicationRepository.findById(id);
        if (!appOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Application not found"));
        }

        try {
            Application application = appOpt.get();
            application.setOfferLetter(file.getBytes());
            application.setOfferLetterFilename(file.getOriginalFilename());
            return ResponseEntity.ok(applicationRepository.save(application));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to upload offer letter"));
        }
    }

    @GetMapping("/{id}/offer")
    public ResponseEntity<byte[]> downloadOfferLetter(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Optional<Application> appOpt = applicationRepository.findById(id);
        if (!appOpt.isPresent() || appOpt.get().getOfferLetter() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Application application = appOpt.get();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + application.getOfferLetterFilename() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(application.getOfferLetter());
    }
}
