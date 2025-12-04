package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for names (person names, contact person).
 * Must not contain special characters.
 * Matches frontend pattern: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/
 * Or regex: ^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$
 */
@Documented
@Constraint(validatedBy = ValidNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {
    String message() default "Tên người liên hệ không hợp lệ.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

