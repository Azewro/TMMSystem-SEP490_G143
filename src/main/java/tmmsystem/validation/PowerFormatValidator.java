package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for power format.
 * Regex matches frontend: /^\d+(\.\d+)?\s*(kw|w|kW|W)?$/i (case-insensitive)
 */
public class PowerFormatValidator implements ConstraintValidator<PowerFormat, String> {
    
    private static final String POWER_REGEX = "^\\d+(\\.\\d+)?\\s*(kw|w|kW|W)?$";
    
    @Override
    public void initialize(PowerFormat constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String power, ConstraintValidatorContext context) {
        if (power == null || power.trim().isEmpty()) {
            return false;
        }
        return power.trim().matches(POWER_REGEX);
    }
}

