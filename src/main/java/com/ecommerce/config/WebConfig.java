package com.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from ./uploads/ directory at /uploads/** URL
        // This is why product images were broken after add/edit —
        // the file was saved but Spring wasn't serving it
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}