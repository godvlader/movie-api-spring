package com.example.movieapi.service;

import ch.qos.logback.classic.Logger;
import com.example.movieapi.controller.MovieController;
import com.example.movieapi.dto.MovieDTO;
import com.example.movieapi.exceptions.MovieNotFoundException;
import com.example.movieapi.model.Movie;
import com.example.movieapi.repository.MovieRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    //constructor for dependency injection
    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public long getMoviesCount() {
        return movieRepository.count();
    }

    //GET /movie -> returns all movies in json structure
    public List<MovieDTO> getAllMoviesSimple() {
        return movieRepository.findAll().stream()
                .map(movie -> new MovieDTO(movie.getId(), movie.getTitle(), movie.getDirector(), movie.getYear()))
                .collect(Collectors.toList());
    }

    //endpoint /movies/{id} in this response. In this way you can give the FE
    //click on each movie to get details of that movie
    public List<MovieDTO> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(movie -> {
                    MovieDTO movieDTO = new MovieDTO(movie.getId(), movie.getTitle(), movie.getDirector(), movie.getYear());
                    //add self-link to each MovieDTO
                    movieDTO.add(linkTo(methodOn(MovieController.class).getMovieById(movie.getId())).withSelfRel());
                    return movieDTO;
                })
                .collect(Collectors.toList());
    }

    //GET /movies/{id} which returns 1 movie with given id.
    public MovieDTO getMovieById(@PathVariable Long id){
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        MovieDTO movieDTO = new MovieDTO(movie.getId(), movie.getTitle(), movie.getDirector(), movie.getYear());

        return movieDTO;
    }

    // POST /movie which has as body a movie and will create that movie

    public ResponseEntity<Movie> addMovie(Movie movie) {
        //should add some input validation normally
        boolean exists = movieRepository.existsByTitleAndDirectorAndYear(movie.getTitle(), movie.getDirector(), movie.getYear());
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty() ||
                movie.getDirector() == null || movie.getDirector().trim().isEmpty() ||
                movie.getYear() < 1900) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (exists) {
            // Log because it doesn't show me the exception in the console
            Logger logger = (Logger) LoggerFactory.getLogger(getClass());
            logger.error("Conflict: Movie already exists with title {}, director {}, and year {}",
                    movie.getTitle(), movie.getDirector(), movie.getYear());

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movie already exists");
        }
        Movie savedMovie = movieRepository.save(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMovie);
    }

    //endpoint called DEL /movie/{id} to remove a movie with given id

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException(id);
        }
        movieRepository.deleteById(id);
    }

    //endpoint called PUT /movie/{id} that will update a movie of given id

    public Movie updateMovie(Long id, Movie updatedMovie) {
        //check if exists
        Movie existingMovie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: " + id));

        //check if it is already in the database
        boolean exists = movieRepository.existsByTitleAndDirectorAndYear(
                updatedMovie.getTitle(), updatedMovie.getDirector(), updatedMovie.getYear());
        if (exists) {
            // Log because it doesn't show me the exception in the console
            Logger logger = (Logger) LoggerFactory.getLogger(getClass());
            logger.error("Conflict: Movie already exists with title {}, director {}, and year {}",
                    existingMovie.getTitle(), existingMovie.getDirector(), existingMovie.getYear());

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movie already exists with the updated information");
        }

        if (updatedMovie.getTitle() == null || updatedMovie.getTitle().trim().isEmpty() ||
                updatedMovie.getDirector() == null || updatedMovie.getDirector().trim().isEmpty() ||
                updatedMovie.getYear() < 1900) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        //update and save
        existingMovie.setTitle(updatedMovie.getTitle());
        existingMovie.setDirector(updatedMovie.getDirector());
        existingMovie.setYear(updatedMovie.getYear());
        movieRepository.save(existingMovie);
        return existingMovie;
    }
}


