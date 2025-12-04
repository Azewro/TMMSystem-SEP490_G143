package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for optional password strength.
 * Only validates if password is not null and not empty.
 * Requirements:
 * - Min 8 characters
 * - No whitespace (regex: ^[^\s]+$)
 * - At least 1 digit (regex: .*\d.*)
 * - At least 1 uppercase letter (regex: .*[A-Z].*)
 */
public class OptionalPasswordStrengthValidator implements ConstraintValidator<OptionalPasswordStrength, String> {
    
    private static final String NO_WHITESPACE_REGEX = "^[^\\s]+$";
    private static final String HAS_DIGIT_REGEX = ".*\\d.*";
    private static final String HAS_UPPERCASE_REGEX = ".*[A-Z].*";
    private static final int MIN_LENGTH = 8;
    
    @Override
    public void initialize(OptionalPasswordStrength constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // If password is null or empty, it's valid (optional field)
        if (password == null || password.trim().isEmpty()) {
            return true;
        }
        
        // Check min length
        if (password.length() < MIN_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Mật khẩu phải có ít nhất 8 ký tự.")
                    .addConstraintViolation();
            return false;
        }
        
        // Check no whitespace
        if (!password.matches(NO_WHITESPACE_REGEX)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Mật khẩu không được chứa khoảng trắng")
                    .addConstraintViolation();
            return false;
        }
        
        // Check has digit
        if (!password.matches(HAS_DIGIT_REGEX)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa")
                    .addConstraintViolation();
            return false;
        }
        
        // Check has uppercase
        if (!password.matches(HAS_UPPERCASE_REGEX)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa")
                    .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}

