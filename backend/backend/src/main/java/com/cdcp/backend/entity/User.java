package com.cdcp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String role; // student, company, admin

    @Lob
    @Column(length = 100000)
    private byte[] resume;
    private String resumeFilename;

    // Extended Student Profile Fields
    private String firstName;
    private String lastName;
    private String department;
    private Double cgpa;
    private Integer backlogCount;

    @Column(length = 1000)
    private String skills;
}