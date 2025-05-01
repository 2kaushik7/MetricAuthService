package com.auth.app.service.implemenation;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.auth.app.constants.AuthConstants;
import com.auth.app.models.MetricAuthRequest;
import com.auth.app.models.MetricAuthResponse;
import com.auth.app.service.MetricsAuthzService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsAuthzServiceImpl implements MetricsAuthzService {

	@Autowired
	private LdapTemplate ldapTemplate;

	@Value("${get.group.names.url}")
	private String getGroupNamesUrl;

	/*
	 * Pending: 
	 * ------- 
	 * group name Exception handling Unit tests
	 * 
	 * Required:
	 * --------
	 * pom.xml, application.props
	 * Models MetricAuthRequest, MetricAuthResponse, MetricsInfo
	 * Interface, ServiceImpl
	 * 
	 */

	/*
	 * Takes username, list of metrics and tenant as input in MetricAuthRequest.
	 * Gives List<MetricAuthResponse> as output. Calls another microservice to get
	 * role group names. User is allowed or not for each metric.
	 */
	@Override
	public List<MetricAuthResponse> authorizationService(MetricAuthRequest metricAuthRequest)
			throws JsonMappingException, JsonProcessingException {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<MetricAuthRequest> request = new HttpEntity<>(metricAuthRequest, headers);
		ResponseEntity<Map<String, String>> response = restTemplate.postForEntity(getGroupNamesUrl, request,
				(Class<Map<String, String>>) (Class<?>) Map.class);
		Map<String, String> metricMap = response.getBody();
		return getGroupLevelAuthorization(metricAuthRequest.getUsername(), metricMap);
	}

	/*
	 * Takes username and groups as input. Gives List<MetricAuthResponse> as output.
	 * Gets userDN (Distinguished Dame) by calling getUserFullDN() Using user's full
	 * dn checks the user exists is the group. After verifying the above scenarios
	 * the List<MetricAuthResponse> is filled with respective status and returned.
	 */
	public List<MetricAuthResponse> getGroupLevelAuthorization(String username, Map<String, String> groups) {
		Map<String, String> result = new HashMap<>();
		List<MetricAuthResponse> metricAuthResponses = new ArrayList<>();

		String userDn = getUserFullDN(username);
		log.info("userDn: " + userDn);
		groups.forEach((metric, group) -> {
			// log.info("Group: "+group);
			if (userDn.isEmpty()) {
				result.put(metric, AuthConstants.USER_NOT_EXIST);
				metricAuthResponses.add(new MetricAuthResponse(metric, group, AuthConstants.USER_NOT_EXIST));
			}
			// log.info("group: " + group);
			boolean groupExists = ldapTemplate
					.search(query().where("objectClass").is("groupOfNames").and("cn").is(group),
							(AttributesMapper<Boolean>) attrs -> true // just confirm it exists
			).stream().findFirst().orElse(false);
			if (groupExists) {
				boolean isMember = ldapTemplate
						.search(query().where("objectClass").is("groupOfNames").and("cn").is(group).and("member")
								.is(userDn), (AttributesMapper<Boolean>) attrs -> true)
						.stream().findFirst().orElse(false);

				result.put(metric, isMember ? AuthConstants.USER_EXISTS : AuthConstants.USER_NOT_EXIST);
				if (!isMember) {
					metricAuthResponses.add(new MetricAuthResponse(metric, group, AuthConstants.USER_NOT_AUTHORIZED));
				}
			} else {
				result.put(metric, AuthConstants.GROUP_NOT_EXIST); // group doesn't exist
				metricAuthResponses.add(new MetricAuthResponse(metric, group, AuthConstants.GROUP_NOT_EXIST));
			}

		});

		return metricAuthResponses;
	}

	/*
	 * Takes username as input Returns userDN (Distinguished Dame) If user is not
	 * found returns an empty string
	 */
	public String getUserFullDN(String username) {
		List<String> userDns = ldapTemplate.search(
				query().where("objectClass").is("inetOrgPerson").and("uid").is(username),
				(ContextMapper<String>) ctx -> ((DirContextAdapter) ctx).getNameInNamespace());

		if (userDns.isEmpty()) {
			return new StringBuilder("").toString();
		}

		return userDns.get(0);
	}

}
