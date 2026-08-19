package com.mcm.craft.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    private String id;

    private String gender;

    private Integer age;

    private Integer height;

    private Integer weight;

    private String bodyType;

    @Column(nullable = false)
    private boolean isIdentified;

    private LocalDateTime identifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Customer create(String id) {
        Customer customer = new Customer();
        customer.id = id;
        customer.isIdentified = false;
        customer.createdAt = LocalDateTime.now();
        return customer;
    }

    public void updateProfile(String gender, Integer age, Integer height, Integer weight, String bodyType) {
        if (gender != null) {
            this.gender = gender;
        }
        if (age != null) {
            this.age = age;
        }
        if (height != null) {
            this.height = height;
        }
        if (weight != null) {
            this.weight = weight;
        }
        if (bodyType != null) {
            this.bodyType = bodyType;
        }
    }

    public void markIdentified() {
        this.isIdentified = true;
        this.identifiedAt = LocalDateTime.now();
    }
}
