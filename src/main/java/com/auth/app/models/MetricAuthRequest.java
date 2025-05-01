package com.auth.app.models;

import java.util.List;

import lombok.Data;

@Data
public class MetricAuthRequest {
	String username;
	String tenant;
	List<String> metrics;
}
