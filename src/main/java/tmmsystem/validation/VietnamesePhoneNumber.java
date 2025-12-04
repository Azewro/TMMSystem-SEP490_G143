package tmmsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Vietnamese phone numbers.
 * Matches frontend regex: /^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$/
 */
@Documented
@Constraint(validatedBy = VietnamesePhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface VietnamesePhoneNumber {
    String message() default "Số điện thoại không hợp lệ.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

