package com.example.movieapi;

import com.example.movieapi.dto.MovieDTO;
import com.example.movieapi.model.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
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

	// Test for getting all movies
	@Test
	public void testGetAllMoviesHateoasLinks() throws Exception {
		ResponseEntity<String> response = restTemplate.getForEntity("/movies", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotEmpty();

		//JsonNode => navigates the JSON returned by endpoints, verifying the structure and content of the response
		JsonNode rootNode = objectMapper.readTree(response.getBody());
		JsonNode moviesList = rootNode.path("_embedded").path("movieDTOList");
		assertTrue(moviesList.isArray());

		//each movie in the list has a self link
		for (JsonNode movie : moviesList) {
			JsonNode selfLink = movie.path("_links").path("self").path("href");
			assertNotNull(selfLink.asText(), "Self link should not be null");
		}

		//verify collection's self link
		JsonNode collectionSelfLink = rootNode.path("_links").path("self").path("href");
		assertNotNull(collectionSelfLink.asText(), "Collection self link should not be null");
	}

	// Test for getting movies count
	@Test
	public void testGetMoviesCount() throws Exception {
		ResponseEntity<String> response = restTemplate.getForEntity("/count", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//get the count from the response
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
	}

	// Test updating a movie
	@Test
	public void testUpdateMovie() {
		//creating a new movie
		Movie preCreatedMovie = new Movie("Original Title", "Original Director", 1990);
		ResponseEntity<Movie> createResponse = restTemplate.postForEntity("/add", preCreatedMovie, Movie.class);
		Long movieId = createResponse.getBody().getId();

		//details which replace the created movie
		Movie updatedDetails = new Movie("Updated Title", "Updated Director", 1995);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Movie> requestEntity = new HttpEntity<>(updatedDetails, headers);

		//update it
		ResponseEntity<Movie> updateResponse = restTemplate.exchange("/update/" + movieId, HttpMethod.PUT, requestEntity, Movie.class);

		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Movie updatedMovie = updateResponse.getBody();
		assertThat(updatedMovie.getTitle()).isEqualTo("Updated Title");
		assertThat(updatedMovie.getDirector()).isEqualTo("Updated Director");
		assertThat(updatedMovie.getYear()).isEqualTo(1995);

		//refetch to see if it has been done correctly
		Movie fetchedMovie = restTemplate.getForObject("/movies/" + movieId, Movie.class);
		assertThat(fetchedMovie.getTitle()).isEqualTo(updatedMovie.getTitle());
		assertThat(fetchedMovie.getDirector()).isEqualTo(updatedMovie.getDirector());
		assertThat(fetchedMovie.getYear()).isEqualTo(updatedMovie.getYear());

	}

	// Test deleting a movie
	private Long createTestMovie(String title, String director, int year) {
		Movie movie = new Movie(title, director, year);
		ResponseEntity<Movie> response = restTemplate.postForEntity("/add", movie, Movie.class);

		//returns id of the new movie
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
		//new movie to be deleted
		Long movieId = createTestMovie("Test Movie", "Test Director", 1999);

		//deleting by id
		restTemplate.delete("/delete/" + movieId);

		//refetch to get the error
		ResponseEntity<String> response = restTemplate.getForEntity("/movies/" + movieId, String.class);

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

