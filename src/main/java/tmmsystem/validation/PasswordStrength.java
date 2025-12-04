package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for password strength.
 * Requirements:
 * - Min 8 characters
 * - No whitespace
 * - At least 1 digit
 * - At least 1 uppercase letter
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    String message() default "Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

