package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Vietnamese phone numbers.
 * Regex matches frontend: /^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$/
 */
public class VietnamesePhoneNumberValidator implements ConstraintValidator<VietnamesePhoneNumber, String> {
    
    private static final String VIETNAMESE_PHONE_REGEX = "^(?:\\+84|84|0)(?:2\\d{1,2}([-.]?)\\d{7,8}|(?:3\\d|5\\d|7\\d|8\\d|9\\d)([-.]?)\\d{3}\\2\\d{4})$";
    
    @Override
    public void initialize(VietnamesePhoneNumber constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return phoneNumber.trim().matches(VIETNAMESE_PHONE_REGEX);
    }
}

