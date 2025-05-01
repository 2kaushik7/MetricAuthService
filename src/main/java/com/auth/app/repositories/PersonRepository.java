package com.auth.app.repositories;

import org.springframework.data.ldap.repository.LdapRepository;

import com.auth.app.models.Person;

public interface PersonRepository extends LdapRepository<Person>{
	 Person findByUsername(String username);
}
