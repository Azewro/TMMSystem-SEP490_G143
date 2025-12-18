package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Vietnamese tax code.
 * Vietnam tax codes are exactly 10 or 13 digits only.
 */
@Documented
@Constraint(validatedBy = TaxCodeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxCode {
    String message() default "Mã số thuế phải là 10 hoặc 13 chữ số.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
