package com.stream.app.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private String id;

    private String title;

    // @OneToMany(mappedBy = "course")
    // private List<Video> list = new ArrayList<>();

}
