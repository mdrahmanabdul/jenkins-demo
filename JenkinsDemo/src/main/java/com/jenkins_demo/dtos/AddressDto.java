package com.jenkins_demo.dtos;

import com.jenkins_demo.validationgrps.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AddressDto {

    @NotBlank(
        groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}, 
        message = "City cannot be blank"
    )
    private String city;

    @NotBlank(
        groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}, 
        message = "Zip code cannot be blank"
    )
    @Pattern(
        regexp = "^\\d{5}$", 
        groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}, 
        message = "Zip code must be exactly 5 digits"
    )
    private String zipCode;
    
    // ... Getters and Setters
}