package com.mshop.web.api.modal;

import lombok.Data;

import java.time.Instant;

@Data
public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String city;
    private Instant createdAt;
}
