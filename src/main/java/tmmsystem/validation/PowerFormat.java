package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for power format.
 * Matches frontend regex: /^\d+(\.\d+)?\s*(kw|w|kW|W)?$/i (case-insensitive)
 */
@Documented
@Constraint(validatedBy = PowerFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PowerFormat {
    String message() default "Công suất không hợp lệ. VD: 5kW, 3kW";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

