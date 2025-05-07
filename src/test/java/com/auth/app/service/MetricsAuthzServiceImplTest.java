package com.auth.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;

import com.auth.app.constants.AuthConstants;
import com.auth.app.models.AuthorizationStatusResponse;
import com.auth.app.models.MetricAuthRequest;
import com.auth.app.models.MetricAuthResponse;
import com.auth.app.models.MetricResponse;
import com.auth.app.service.implemenation.MetricsAuthzServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
	
	@Spy
    @InjectMocks
    private MetricsAuthzServiceImpl service;

    @Mock
    private LdapTemplate ldapTemplate;

    private static final String USER_DN = "uid=test,ou=people,dc=example,dc=com";
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void testGetUserFullDnReturnsDn() {
        List<String> result = List.of(USER_DN);
        Mockito.when(ldapTemplate.search(
                Mockito.any(),
                Mockito.any(ContextMapper.class)))
                .thenReturn(result);

        String userDn = service.getUserFullDN("testuser");
        assertEquals(USER_DN, userDn);
    }

    @Test
    void testGetUserFullDnReturnsEmptyWhenUserNotFound() {
        Mockito.when(ldapTemplate.search(
                Mockito.any(),
                Mockito.any(ContextMapper.class)))
                .thenReturn(Collections.emptyList());

        String userDn = service.getUserFullDN("unknown");
        assertEquals("", userDn);
    }

    @Test
    void testGetUserFullDnThrowsOnLdapFailure() {
        CommunicationException ex = new CommunicationException(null);
        ex.initCause(new ConnectException("Connection failed"));

        Mockito.when(ldapTemplate.search(
                Mockito.any(),
                Mockito.any(ContextMapper.class)))
                .thenThrow(ex);

        assertThrows(CommunicationException.class, () -> service.getUserFullDN("testuser"));
    }
    
    @Test
    void testGroupAuthorizationUserNotExist() {
        MetricResponse input = new MetricResponse(List.of(new MetricResponse.MetricEntry("metric1", "group1")));

        Mockito.doReturn("").when(service).getUserFullDN("unknown");

        List<MetricAuthResponse> result = service.getGroupLevelAuthorization("unknown", input);

        assertEquals(1, result.size());
        assertEquals("User record not found for unknown", result.get(0).getStatus());
    }

    @Test
    void testGroupAuthorizationGroupNotExist() {
        MetricResponse input = new MetricResponse(List.of(new MetricResponse.MetricEntry("metric1", "group1")));

        Mockito.doReturn(USER_DN).when(service).getUserFullDN("testuser");
        Mockito.when(ldapTemplate.search(Mockito.any(), Mockito.any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList()); // group not found

        List<MetricAuthResponse> result = service.getGroupLevelAuthorization("testuser", input);

        assertEquals(1, result.size());
        assertEquals(AuthConstants.GROUP_NOT_EXIST, result.get(0).getStatus());
    }

    @Test
    void testGroupAuthorizationUserNotAuthorized() {
        MetricResponse input = new MetricResponse(List.of(new MetricResponse.MetricEntry("metric1", "group1")));

        Mockito.doReturn(USER_DN).when(service).getUserFullDN("testuser");
        Mockito.when(ldapTemplate.search(Mockito.any(), Mockito.any(AttributesMapper.class)))
                .thenReturn(List.of(true)) // group exists
                .thenReturn(Collections.emptyList()); // user not in group

        List<MetricAuthResponse> result = service.getGroupLevelAuthorization("testuser", input);

        assertEquals(1, result.size());
        assertEquals(AuthConstants.USER_NOT_AUTHORIZED, result.get(0).getStatus());
    }

    @Test
    void testGroupAuthorizationLdapFailure() {
    	
    	CommunicationException ex = new CommunicationException(null);
        ex.initCause(new ConnectException("Connection failed"));
        MetricResponse input = new MetricResponse(List.of(new MetricResponse.MetricEntry("metric1", "group1")));

        Mockito.doReturn(USER_DN).when(service).getUserFullDN("testuser");
        Mockito.when(ldapTemplate.search(Mockito.any(), Mockito.any(AttributesMapper.class)))
                .thenThrow(ex);

        List<MetricAuthResponse> result = service.getGroupLevelAuthorization("testuser", input);

        assertEquals(1, result.size());
        assertEquals("LDAP connection failed while checking group group1 for user testuser", result.get(0).getStatus());
    }
    
    @Test
    void testGroupAuthorizationUserFullDnLdapFailure() {
    	
    	CommunicationException ex = new CommunicationException(null);
        ex.initCause(new ConnectException("Connection failed"));
        MetricResponse input = new MetricResponse(List.of(new MetricResponse.MetricEntry("metric1", "group1")));

        Mockito.doThrow(ex).doReturn(USER_DN).when(service).getUserFullDN("testuser");

        List<MetricAuthResponse> result = service.getGroupLevelAuthorization("testuser", input);

        assertEquals(1, result.size());
        assertEquals("LDAP connection failed while checking for user testuser", result.get(0).getStatus());
    }
    
    @Test
    void testAuthorizationService_WithUnauthorizedMetric() throws Exception {
        MetricAuthRequest request = new MetricAuthRequest();
        request.setUsername("user");
        request.setTenant("tenant");
        request.setMetrics(List.of("metric1"));

        MetricResponse metricResponse = new MetricResponse(); // Populate as needed
        metricResponse.setResult(new ArrayList<>());
        String jsonResponse = mapper.writeValueAsString(metricResponse);

        List<MetricAuthResponse> authResponses = List.of(
            new MetricAuthResponse("metric1","test_group", AuthConstants.USER_NOT_AUTHORIZED)
        );

        // Mocking
        Mockito.when(service.getLdapAccessGroup("user", "tenant", 'Y', List.of("metric1")))
               .thenReturn(jsonResponse);

        Mockito.when(service.getGroupLevelAuthorization("user", metricResponse))
               .thenReturn(authResponses);

        // Act
        String jsonResult = service.authorizationService(request);

        AuthorizationStatusResponse response = mapper.readValue(jsonResult, AuthorizationStatusResponse.class);

        // Assert
        assertFalse(response.isStatus());
        assertEquals(1, response.getResponses().size());
        assertEquals(AuthConstants.USER_NOT_AUTHORIZED, response.getResponses().get(0).getStatus());
    }

    @Test
    void testAuthorizationService_AllAuthorized() throws Exception {
        MetricAuthRequest request = new MetricAuthRequest();
        request.setUsername("user");
        request.setTenant("tenant");
        request.setMetrics(List.of("metric1"));

        MetricResponse metricResponse = new MetricResponse(); // Populate as needed
        metricResponse.setResult(new ArrayList<>());
        String jsonResponse = mapper.writeValueAsString(metricResponse);

        List<MetricAuthResponse> authResponses = List.of(
            new MetricAuthResponse("metric1","test_group", "AUTHORIZED")
        );

        // Mocking
        Mockito.when(service.getLdapAccessGroup("user", "tenant", 'Y', List.of("metric1")))
               .thenReturn(jsonResponse);

        Mockito.when(service.getGroupLevelAuthorization("user", metricResponse))
               .thenReturn(authResponses);

        // Act
        String jsonResult = service.authorizationService(request);

        AuthorizationStatusResponse response = mapper.readValue(jsonResult, AuthorizationStatusResponse.class);

        // Assert
        assertTrue(response.isStatus());
        assertTrue(response.getResponses().isEmpty()); // default empty list
    }


}

