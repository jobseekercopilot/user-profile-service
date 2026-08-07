package com.jobseekercopilot.userprofileservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userProfileOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("Jobseeker Copilot - User Profile API")
                        .description("""
                                Service for managing user profiles in the Jobseeker Copilot application.

                                User profile fields:
                                - userId: Unique user identifier
                                - skills: List of skills
                                - aspirations: Target roles and weekly hours preferences
                                - workPreferences: Location and commute preferences
                                - qualifications: Educational qualifications (name, issuing body, status, grade)
                                - roles: Work history (job title, employer, dates, responsibilities)
                                - postcodeLocation: Postcode, region, district, coordinates
                                """)
                        .version("2.1.0")
                        .contact(new Contact()
                                .name("Jobseeker Copilot"))
                        .license(new License()
                                .name("Proprietary")));
    }
}
