package com.auth.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig{
    
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .csrf().disable()  // Disable CSRF if not needed
            .formLogin().disable()  // Disable the login form
            .httpBasic().disable(); // Disable basic auth

        return http.build();
    }

    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .ldapAuthentication()
            .userDnPatterns("uid={0},ou=People")
            .groupSearchBase("ou=Groups")
            .contextSource()
            .url("ldap://openldap:389/dc=mycompany,dc=com")
            .managerDn("cn=admin,dc=mycompany,dc=com")
            .managerPassword("adminpassword");
    }
}
