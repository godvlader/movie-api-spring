package com.example.movieapi.repository;

import com.example.movieapi.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository //provides automatic translation of exceptions
public interface MovieRepository extends JpaRepository<Movie, Long> {
    boolean existsByTitleAndDirectorAndYear(String title, String director, int year);
}
