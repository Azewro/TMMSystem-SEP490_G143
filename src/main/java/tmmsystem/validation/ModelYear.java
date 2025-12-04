package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for model year.
 * Must be 4 digits, between 1900 and current year + 1.
 * Matches frontend: /^\d{4}$/ and range validation
 */
@Documented
@Constraint(validatedBy = ModelYearValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelYear {
    String message() default "Năm sản xuất không hợp lệ. Phải là năm từ 1900 đến năm hiện tại + 1";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

