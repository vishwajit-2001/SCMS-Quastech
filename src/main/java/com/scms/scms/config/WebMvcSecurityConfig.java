package com.scms.scms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    @Autowired
    private SessionAccessInterceptor sessionAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAccessInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/register",
                        "/overview",
                        "/courses",
                        "/contact",
                        "/login-student",
                        "/login-teacher",
                        "/login-admin",
                        "/login-placement",
                        "/placement-login",
                        "/placement-logout",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.woff",
                        "/**/*.woff2"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectUploads = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "static", "uploads")
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(projectUploads, "file:uploads/", "classpath:/static/uploads/");
    }
}
