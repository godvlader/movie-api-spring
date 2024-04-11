package com.example.movieapi;

import com.example.movieapi.dto.MovieDTO;
import com.example.movieapi.model.Movie;
import com.example.movieapi.service.MovieService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MovieApiApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private MovieService movieService;

	@Autowired
	private MockMvc mockMvc;

	// Util for converting JSON response to objects
	private final ObjectMapper objectMapper = new ObjectMapper();

	// Test for getting all movies in simple format
	@Test
	public void testGetAllMovies() throws Exception {
		ResponseEntity<String> response = restTemplate.getForEntity("/simple", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		List<MovieDTO> movies = objectMapper.readValue(response.getBody(), new TypeReference<List<MovieDTO>>() {
		});
		assertThat(movies).isNotEmpty(); //there are movies in the database
	}

	// Test for getting movies count
	@Test
	public void testGetMoviesCount() throws Exception {
		ResponseEntity<String> response = restTemplate.getForEntity("/count", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		// Extract the count from the response
		Map<String, Long> countResponse = objectMapper.readValue(response.getBody(), new TypeReference<>() {
        });
		assertThat(countResponse).containsKey("number of movies in the database");
		Long count = countResponse.get("number of movies in the database");
		assertThat(count).isNotNull();
	}

	// Test adding a movie successfully
	@Test
	public void testAddMovieSuccess() throws Exception {
		Movie newMovie = new Movie("Test Movie", "Test Director", 2021);
		ResponseEntity<Movie> response = restTemplate.postForEntity("/add", newMovie, Movie.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Test Movie");
		// Cleanup if necessary
	}

	// Test adding a duplicate movie
	@Test
	public void testAddDuplicateMovie() {
		//request body as a Map for clearer JSON structure
		Map<String, Object> movieMap = new HashMap<>();
		movieMap.put("title", "The Shawshank Redemption");
		movieMap.put("director", "Frank Darabont");
		movieMap.put("year", 1994);

		//send with application/json content type
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(movieMap, headers);

		ResponseEntity<String> response = restTemplate.postForEntity("/add", request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		String expectedErrorMessage = "Conflict";
		assertThat(response.getBody()).contains(expectedErrorMessage);
	}

	//test adding incomplete movie
	@Test
	public void testAddMovieWithMissingFields() {
		Map<String, Object> incompleteMovie = new HashMap<>();
		incompleteMovie.put("title", "Incomplete Movie");
		//missing 'director' and 'year'

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(incompleteMovie, headers);

		ResponseEntity<String> response = restTemplate.postForEntity("/add", request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void testAddMovieWithTypeMismatch() {
		Map<String, Object> movie = new HashMap<>();
		movie.put("title", "Type Mismatch");
		movie.put("director", "Test Director");
		movie.put("year", "Two Thousand Twenty"); // Incorrect data type for year

		ResponseEntity<String> response = restTemplate.postForEntity("/add", movie, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		// Additionally, assert the response body contains a message about the type mismatch
	}

	// Test updating a movie
	@Test
	public void testUpdateMovie() {
		// 1. Pre-create a movie
		Movie preCreatedMovie = new Movie("Original Title", "Original Director", 1990);
		ResponseEntity<Movie> createResponse = restTemplate.postForEntity("/add", preCreatedMovie, Movie.class);
		Long movieId = createResponse.getBody().getId();

		// 2. Prepare updated movie details
		Movie updatedDetails = new Movie("Updated Title", "Updated Director", 1995);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Movie> requestEntity = new HttpEntity<>(updatedDetails, headers);

		// 3. Make a PUT request to update the movie
		ResponseEntity<Movie> updateResponse = restTemplate.exchange("/update/" + movieId, HttpMethod.PUT, requestEntity, Movie.class);

		// 4. Verify the response
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Movie updatedMovie = updateResponse.getBody();
		assertThat(updatedMovie.getTitle()).isEqualTo("Updated Title");
		assertThat(updatedMovie.getDirector()).isEqualTo("Updated Director");
		assertThat(updatedMovie.getYear()).isEqualTo(1995);

		// 5. Fetch the updated movie and verify changes were persisted
		Movie fetchedMovie = restTemplate.getForObject("/movies/" + movieId, Movie.class);
		assertThat(fetchedMovie.getTitle()).isEqualTo(updatedMovie.getTitle());
		assertThat(fetchedMovie.getDirector()).isEqualTo(updatedMovie.getDirector());
		assertThat(fetchedMovie.getYear()).isEqualTo(updatedMovie.getYear());

		// Cleanup if necessary
		//restTemplate.delete("/delete/" + movieId);
	}

	// Test deleting a movie
	private Long createTestMovie(String title, String director, int year) {
		Movie movie = new Movie(title, director, year);
		ResponseEntity<Movie> response = restTemplate.postForEntity("/add", movie, Movie.class);

		// Return the ID of the created movie
		return Objects.requireNonNull(response.getBody()).getId();
	}

	// Test update non existent movie
	@Test
	public void testUpdateNonexistentMovie() {
		Long nonexistentMovieId = 9999L;
		Movie updateDetails = new Movie("Nonexistent Movie", "Nonexistent Director", 2000);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Movie> requestEntity = new HttpEntity<>(updateDetails, headers);

		//update the movie
		ResponseEntity<String> response = restTemplate.exchange("/update/" + nonexistentMovieId, HttpMethod.PUT, requestEntity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	//Test delete existent movie
	@Test
	public void testDeleteExistingMovie() {
		// Create a test movie and get its ID
		Long movieId = createTestMovie("Test Movie", "Test Director", 1999);

		// Now delete the movie
		restTemplate.delete("/delete/" + movieId);

		// Try to fetch the movie again to verify it's been deleted
		ResponseEntity<String> response = restTemplate.getForEntity("/movies/" + movieId, String.class);

		// Assert that fetching the movie now results in a 404 Not Found
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	//Test delete existent movie
	@Test
	public void testDeleteNonExistingMovie() throws Exception {
		mockMvc.perform(delete("/delete/{id}", 999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Movie not found with id: 999"));
	}

}

