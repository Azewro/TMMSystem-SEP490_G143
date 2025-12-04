package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for names (person names, contact person).
 * Must not contain special characters.
 * Regex matches frontend: ^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$
 */
public class ValidNameValidator implements ConstraintValidator<ValidName, String> {
    
    private static final String VALID_NAME_REGEX = "^[^!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*$";
    
    @Override
    public void initialize(ValidName constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.trim().matches(VALID_NAME_REGEX);
    }
}

