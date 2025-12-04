package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for expected delivery date.
 * Must be at least 30 days from today.
 * Matches frontend: CreateRfqPage.jsx - getMinExpectedDeliveryDate
 */
@Documented
@Constraint(validatedBy = ExpectedDeliveryDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpectedDeliveryDate {
    String message() default "Ngày giao hàng phải ít nhất 30 ngày kể từ hôm nay.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

