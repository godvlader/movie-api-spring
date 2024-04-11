package com.example.movieapi.dto;

import org.springframework.hateoas.RepresentationModel;

public class MovieDTO extends RepresentationModel<MovieDTO> {
    private Long id;
    private String title;
    private String director;
    private int year;

    //default construct =>
    public MovieDTO() {}

    public MovieDTO( Long id, String title, String director, int year) {
        this.id=id;
        this.title = title;
        this.director = director;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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