package com.auth.app.models;

//LDAP Name class
import javax.naming.Name;

//Core LDAP annotations
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entry(
 base = "ou=People",
 objectClasses = { "inetOrgPerson", "posixAccount", "shadowAccount" }
)
@Data
public class Person {
 @Id
 @JsonIgnore
 private Name id;
 
 @Attribute(name = "uid")
 private String username;
 
 @Attribute(name = "cn")
 private String fullName;
 
 // Additional fields with their LDAP mappings
 @Attribute(name = "sn")
 private String lastName;
 
 @Attribute(name = "mail")
 private String email;
 
 @Attribute(name = "userPassword")
 private String password;
 
 // Getters and setters for all fields
 // ...
}
