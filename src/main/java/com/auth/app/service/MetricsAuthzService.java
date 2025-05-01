package com.auth.app.service;

import java.util.List;
import java.util.Map;

import com.auth.app.models.MetricAuthRequest;
import com.auth.app.models.MetricAuthResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface MetricsAuthzService {
	public List<MetricAuthResponse> authorizationService(MetricAuthRequest metricAuthRequest) throws JsonMappingException, JsonProcessingException;
}
