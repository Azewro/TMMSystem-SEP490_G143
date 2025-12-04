package tmmsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration to handle Hibernate lazy-loading proxies.
 * This fixes the "Type definition error: ByteBuddyInterceptor" serialization issue.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Disable forcing lazy loading - will serialize null for uninitialized lazy properties
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Serialize identifier for lazy but not loaded associations
        module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        return module;
    }

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        objectMapper.registerModule(hibernate6Module());
        return objectMapper;
    }
}
