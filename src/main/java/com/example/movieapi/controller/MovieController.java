package com.example.movieapi.controller;

import com.example.movieapi.dto.MovieDTO;
import com.example.movieapi.model.Movie;
import com.example.movieapi.service.MovieService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getMoviesCount() {
        long count = movieService.getMoviesCount();
        //explicit mapping for the json structure
        Map<String, Long> response = Collections.singletonMap("number of movies in the database", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/simple")
    public ResponseEntity<List<MovieDTO>> getAllMoviesSimple() {
        List<MovieDTO> movieDTOs = movieService.getAllMoviesSimple();
        return ResponseEntity.ok(movieDTOs);
    }


    @GetMapping("/movies")
    public ResponseEntity<CollectionModel<MovieDTO>> getAllMovies() {
        List<MovieDTO> movieDTOs = movieService.getAllMovies();

        // Create a CollectionModel to wrap the list of MovieDTOs and add links
        CollectionModel<MovieDTO> collectionModel = CollectionModel.of(movieDTOs);
        collectionModel.add(linkTo(methodOn(MovieController.class).getAllMovies()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/movies/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id){
        MovieDTO movie = movieService.getMovieById(id);
        return ResponseEntity.ok(movie);
    }

    @PostMapping("/add")
    public ResponseEntity<Movie> addMovie(@RequestBody Movie movie) {
        Movie savedMovie = movieService.addMovie(movie).getBody();
        return new ResponseEntity<>(savedMovie, HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @RequestBody Movie movieDetails) {
        Movie updatedMovie = movieService.updateMovie(id, movieDetails);
        return ResponseEntity.ok(updatedMovie);
    }

}
