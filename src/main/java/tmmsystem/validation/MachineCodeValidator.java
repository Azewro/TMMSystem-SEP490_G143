package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for machine code.
 * Regex matches frontend: /^[A-Z0-9_-]+$/i (case-insensitive)
 */
public class MachineCodeValidator implements ConstraintValidator<MachineCode, String> {
    
    private static final String MACHINE_CODE_REGEX = "^[A-Z0-9_-]+$";
    
    @Override
    public void initialize(MachineCode constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return code.trim().toUpperCase().matches(MACHINE_CODE_REGEX);
    }
}

