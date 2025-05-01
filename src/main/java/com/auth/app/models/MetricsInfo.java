package com.auth.app.models;

import lombok.Data;

@Data
public class MetricsInfo {
	private String metricName;
	private String tenant;
    private String roleGroupName;

}
