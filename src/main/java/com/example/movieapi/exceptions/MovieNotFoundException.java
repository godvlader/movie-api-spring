package com.example.movieapi.exceptions;
public class MovieNotFoundException extends RuntimeException {

    private Long movieId;
    public MovieNotFoundException(Long movieId) {
        super("Movie not found with id: " + movieId);
        this.movieId = movieId;
    }

}


