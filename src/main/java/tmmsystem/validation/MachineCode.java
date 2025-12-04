package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for machine code.
 * Matches frontend regex: /^[A-Z0-9_-]+$/i (case-insensitive)
 */
@Documented
@Constraint(validatedBy = MachineCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MachineCode {
    String message() default "Mã máy không hợp lệ. VD: WEAV-001";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

