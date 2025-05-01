package com.auth.app.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricAuthResponse {
	String metric;
	String group;
	String status;
}
