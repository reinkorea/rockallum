package com.mycompany.myapp.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.mycompany.myapp.domain.Album;
import com.mycompany.myapp.domain.Band;
import com.mycompany.myapp.domain.Review;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AlbumRepository;
import com.mycompany.myapp.repository.BandRepository;
import com.mycompany.myapp.repository.ReviewRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.search.ReviewSearchRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.web.rest.util.HeaderUtil;
import com.mycompany.myapp.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

/**
 * REST controller for managing Review.
 */
@RestController
@RequestMapping("/api")
public class ReviewResource {

    private final Logger log = LoggerFactory.getLogger(ReviewResource.class);

    @Inject
    private ReviewRepository reviewRepository;

    @Inject
    private ReviewSearchRepository reviewSearchRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AlbumRepository albumRepository;

    @Inject
    private BandRepository bandRepository;

    /**
     * POST  ALBUM REVIEW /reviews -> Create a new review.
     */
    @RequestMapping(value = "/album/{id}/reviews",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Review> createReview(@Valid @RequestBody Review review, @PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to save Review : {}", review);
        if (review.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("review", "idexists", "A new review cannot already have an ID")).body(null);
        }
        User user = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get();
        Album album = albumRepository.findOne(id);

        review.setUser(user);
        review.setAlbum(album);
        ZonedDateTime today = ZonedDateTime.now();
        review.setReviewDate(today);

        Review result = reviewRepository.save(review);
        reviewSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/reviews/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("review", result.getId().toString()))
            .body(result);
    }

    /**
     * POST  BAND REVIEW /reviews -> Create a new review.
     */

    @RequestMapping(value = "/band/{id}/reviews",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Review> createBandReview(@Valid @RequestBody Review review, @PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to save Review : {}", review);
        if (review.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("review", "idexists", "A new review cannot already have an ID")).body(null);
        }
        User user = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get();
        Band band = bandRepository.findOne(id);

        review.setUser(user);
        review.setBand(band);
        ZonedDateTime today = ZonedDateTime.now();
        review.setReviewDate(today);

        Review result = reviewRepository.save(review);
        reviewSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/reviews/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("review", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /reviews -> Updates an existing review.
     */
    @RequestMapping(value = "/reviews",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Review> updateReview(@Valid @RequestBody Review review, @PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to update Review : {}", review);
        if (review.getId() == null) {
            return createReview(review, id);
        }
        Review result = reviewRepository.save(review);
        reviewSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("review", review.getId().toString()))
            .body(result);
    }

    /**
     * GET  /reviews -> get all the reviews.
     */
    @RequestMapping(value = "/reviews",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Review>> getAllReviews(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Reviews");
        Page<Review> page = reviewRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/reviews");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /reviews -> get all the reviews.
     */
    @RequestMapping(value = "/reviewsbyband",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Review> getAllReviewsByBand() {
        log.debug("REST request to get all Reviews");
        return reviewRepository.findByUserIsCurrentUserAndBand();
    }

/*    *//**
     * GET  /reviews -> get all the reviews.
     *//*
    @RequestMapping(value = "/reviewsbyalbum",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Review> getAllReviewsByAlbum() {
        log.debug("REST request to get all Reviews");
        return reviewRepository.findByUserIsCurrentUserAndAlbum();
    }*/

    /**
     * GET  /reviews/:id -> get the "id" review.
     */
    @RequestMapping(value = "/reviews/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Review> getReview(@PathVariable Long id) {
        log.debug("REST request to get Review : {}", id);
        Review review = reviewRepository.findOne(id);
        return Optional.ofNullable(review)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /reviews/:id -> delete the "id" review.
     */
    @RequestMapping(value = "/reviews/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        log.debug("REST request to delete Review : {}", id);
        reviewRepository.delete(id);
        reviewSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("review", id.toString())).build();
    }

    /**
     * SEARCH  /_search/reviews/:query -> search for the review corresponding
     * to the query.
     */
    @RequestMapping(value = "/_search/reviews/{query:.+}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Review> searchReviews(@PathVariable String query) {
        log.debug("REST request to search Reviews for query {}", query);
        return StreamSupport
            .stream(reviewSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }

    /**
     * GET  /reviews/:id -> get the "id" review.
     */
    @Transactional
    @RequestMapping(value = "/band/{id}/reviews",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Review>> getReviewByBand(@PathVariable Long id) {
        log.debug("REST request to get Review : {}", id);
        List<Review> review = reviewRepository.findReviewsByBand(id);
        return new ResponseEntity<>(review, HttpStatus.OK);
    }

    /**
     * GET  /reviews/:id -> get the "id" review.
     */
    @Transactional
    @RequestMapping(value = "/album/{id}/reviews",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Review>> getReviewByAlbum(@PathVariable Long id) {
        log.debug("REST request to get Review : {}", id);
        List<Review> review = reviewRepository.findReviewsByAlbum(id);
        return new ResponseEntity<>(review, HttpStatus.OK);
    }
}
