package com.cdcp.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private String company;
    private String location;
    private Integer salary;

    @Column(length = 1000)
    private String requirements;

    // Eligibility Criteria (from SRS FR-J2)
    private Double requiredCgpa;
    private Integer maxBacklogs;
    private String applicationDeadline;
}
