package com.auth.app.service.implemenation;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import com.auth.app.constants.AuthConstants;
import com.auth.app.models.MetricAuthRequest;
import com.auth.app.models.MetricAuthResponse;
import com.auth.app.models.MetricResponse;
import com.auth.app.service.MetricsAuthzService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsAuthzServiceImpl implements MetricsAuthzService {

	@Autowired
	private LdapTemplate ldapTemplate;

	@Value("${get.group.names.url}")
	private String getGroupNamesUrl;

	@Override
	public List<MetricAuthResponse> authorizationService(MetricAuthRequest metricAuthRequest)
			throws JsonMappingException, JsonProcessingException {

		String json = "{ \"result\": [ { \"metric\": \"memory_usage\", \"ldap_group_name\": \"bd_adcv_restricted\" }, { \"metric\": \"svlt_tx_min\", \"ldap_group_name\": \"bd_t1_analyst\" }, { \"metric\": \"system_memory_in_use_metric\", \"ldap_group_name\": \"bd_t1_analyst\" } ] }";
		ObjectMapper mapper = new ObjectMapper();
		MetricResponse metricList = mapper.readValue(json, MetricResponse.class);
			return getGroupLevelAuthorization(metricAuthRequest.getUsername(), metricList);
	}


	public List<MetricAuthResponse> getGroupLevelAuthorization(String username, MetricResponse groups) {
	    List<MetricAuthResponse> metricAuthResponses = new ArrayList<>();

	    String userDn;
	    try {
	        userDn = getUserFullDN(username);
	    } catch (CommunicationException e) {
	        log.error("LDAP connection failed while fetching user DN for {}", username, e);
	        groups.getResult().forEach(m ->
	            metricAuthResponses.add(new MetricAuthResponse(m.getMetric(), m.getLdapGroupName(), "LDAP connection failed while checking for user "+username))
	        );
	        return metricAuthResponses;
	    }

	    log.info("userDn: " + userDn);

	    for (MetricResponse.MetricEntry m : groups.getResult()) {
	        String metric = m.getMetric();
	        String group = m.getLdapGroupName();

	        if (userDn.isEmpty()) {
	            metricAuthResponses.add(new MetricAuthResponse(metric, group, "User record not found for "+username));
	            continue;
	        }

	        try {
	            boolean groupExists = ldapTemplate
	                .search(query().where("objectClass").is("groupOfNames").and("cn").is(group),
	                        (AttributesMapper<Boolean>) attrs -> true)
	                .stream().findFirst().orElse(false);

	            if (groupExists) {
	                boolean isMember = ldapTemplate
	                    .search(query().where("objectClass").is("groupOfNames").and("cn").is(group).and("member")
	                            .is(userDn), (AttributesMapper<Boolean>) attrs -> true)
	                    .stream().findFirst().orElse(false);

	                if (!isMember) {
	                    metricAuthResponses.add(new MetricAuthResponse(metric, group, AuthConstants.USER_NOT_AUTHORIZED));
	                }
	            } else {
	                metricAuthResponses.add(new MetricAuthResponse(metric, group, AuthConstants.GROUP_NOT_EXIST));
	            }
	        } catch (CommunicationException e) {
	            log.error("LDAP connection failed while checking group {} for user {}", group, username, e);
	            metricAuthResponses.add(new MetricAuthResponse(metric, group, "LDAP connection failed while checking group "+group+" for user "+username));
	        }
	    }

	    return metricAuthResponses;
	}


	/*
	 * Takes username as input Returns userDN (Distinguished Dame) If user is not
	 * found returns an empty string
	 */
	public String getUserFullDN(String username) {
	    try {
	        List<String> userDns = ldapTemplate.search(
	            query().where("objectClass").is("inetOrgPerson").and("uid").is(username),
	            (ContextMapper<String>) ctx -> ((DirContextAdapter) ctx).getNameInNamespace()
	        );

	        return userDns.isEmpty() ? "" : userDns.get(0);
	    } catch (CommunicationException e) {
	        log.error("Failed to connect to LDAP while resolving userDN for user: {}", username, e);
	        throw e; // Let the calling method decide how to handle it
	    }
	}


}
