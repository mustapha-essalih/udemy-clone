package com.dev.lms.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sections")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "section_id")
    private UUID sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

}
