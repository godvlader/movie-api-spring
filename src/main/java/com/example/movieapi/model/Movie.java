package com.example.movieapi.model;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="Movies")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String director;
    @Column(name = "\"year\"")
    private int year;

    //AllArgsConstructor takes the place of all this => when all class fields are initialized at the object creation
    //RequiredArgsConstructor => for required and optional fields => enforces initialization (through the constructor) only for required fields

        public Movie(String title, String director, int year) {
            this.title = title;
            this.director = director;
            this.year = year;
        }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
