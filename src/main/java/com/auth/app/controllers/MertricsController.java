package com.auth.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.app.models.MetricAuthRequest;
import com.auth.app.models.Person;
import com.auth.app.repositories.PersonRepository;
import com.auth.app.service.MetricsAuthzService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics controller", description = "Metrics Controller to fetch group names")
@Slf4j
public class MertricsController {

	@Autowired
	private PersonRepository personRepository;

	private MetricsAuthzService metricsAuthzService;

	@Autowired
	MertricsController(MetricsAuthzService metricsAuthzService) {
		this.metricsAuthzService = metricsAuthzService;
	}

	/*
	 * Takes MetricAuthRequest as input which contains username and list of metrics
	 * Returns a map which contains group name and the user authorization status as
	 * value
	 */
	@PostMapping("/group")
	@Operation(summary = "Get Authorization status for user", description = "Authorization is verified if user can access the metrics")
	public ResponseEntity<String> getMetricInfo(@RequestBody MetricAuthRequest metricAuthRequest)
			throws JsonMappingException, JsonProcessingException {
		log.info("username: " + metricAuthRequest.getUsername());
		metricAuthRequest.getMetrics().forEach(metric -> System.out.println(metric));
		return ResponseEntity.ok(metricsAuthzService.authorizationService(metricAuthRequest));
	}

	@GetMapping("/{username}")
	public Person getUser(@PathVariable String username) {
		return personRepository.findByUsername(username);
	}

	@GetMapping
	public Iterable<Person> getAllUsers() {
		return personRepository.findAll();
	}

}
