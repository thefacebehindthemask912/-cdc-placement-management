package com.cdcp.backend.config;

import com.cdcp.backend.entity.Job;
import com.cdcp.backend.entity.User;
import com.cdcp.backend.repository.JobRepository;
import com.cdcp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.count() == 0) {
                User student = new User(); student.setEmail("student"); student.setPassword("student123"); student.setRole("student");
                User company = new User(); company.setEmail("company"); company.setPassword("company123"); company.setRole("company");
                User admin = new User(); admin.setEmail("admin"); admin.setPassword("admin123"); admin.setRole("admin");

                userRepository.saveAll(Arrays.asList(student, company, admin));

                System.out.println("Inserted sample users: student/student123, company/company123, admin/admin123");
            }

            // Also seed jobs if there are none
            if (jobRepository.count() == 0) {
                Job job1 = new Job(); job1.setTitle("Software Engineer"); job1.setDescription("Develop scalable backend systems using Spring Boot and Java."); job1.setCompany("company"); job1.setLocation("Bangalore"); job1.setSalary(800000); job1.setRequirements("Java, Spring Boot, SQL"); job1.setRequiredCgpa(7.0); job1.setMaxBacklogs(0);
                Job job2 = new Job(); job2.setTitle("Frontend Developer"); job2.setDescription("Build amazing user interfaces using React and TypeScript."); job2.setCompany("company"); job2.setLocation("Remote"); job2.setSalary(600000); job2.setRequirements("React, TypeScript, CSS"); job2.setRequiredCgpa(6.5); job2.setMaxBacklogs(1);
                jobRepository.saveAll(Arrays.asList(job1, job2));
                System.out.println("Inserted sample jobs.");
            }
        };
    }
}
