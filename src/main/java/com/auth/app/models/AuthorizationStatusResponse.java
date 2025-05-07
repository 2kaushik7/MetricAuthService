package com.auth.app.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AuthorizationStatusResponse {
	private boolean status = true;
	private List<MetricAuthResponse> responses = new ArrayList<>();
}
