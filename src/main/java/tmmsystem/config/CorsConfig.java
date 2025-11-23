package tmmsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // áp dụng cho tất cả endpoint
                        .allowedOriginPatterns("*") // cho phép mọi origin
                        .allowedMethods("*") // GET, POST, PUT, DELETE...
                        .allowedHeaders("*") // mọi header
                        .exposedHeaders("Authorization", "Content-Disposition") // expose header cho FE
                        .allowCredentials(true); // cho phép cookie/token
            }
        };
    }
}