package com.auth.app.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricResponse {
    private List<MetricEntry> result;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricEntry {
        private String metric;
        @JsonProperty(value = "ldap_group_name",required = true)
        private String ldapGroupName;
    }
}

