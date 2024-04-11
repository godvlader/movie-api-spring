package com.example.movieapi.repository;

import com.example.movieapi.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // You can define custom query methods here
    boolean existsByTitleAndDirectorAndYear(String title, String director, int year);

    boolean existsByTitleAndDirectorAndYearAndId(String title, String director, int year, Long id);
}
