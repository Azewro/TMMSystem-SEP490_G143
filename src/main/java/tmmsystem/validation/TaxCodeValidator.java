package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Vietnamese tax code.
 * Regex matches frontend: /^[0-9]{10,13}$/
 */
public class TaxCodeValidator implements ConstraintValidator<TaxCode, String> {
    
    private static final String TAX_CODE_REGEX = "^[0-9]{10,13}$";
    
    @Override
    public void initialize(TaxCode constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String taxCode, ConstraintValidatorContext context) {
        if (taxCode == null || taxCode.trim().isEmpty()) {
            return false;
        }
        return taxCode.trim().matches(TAX_CODE_REGEX);
    }
}

