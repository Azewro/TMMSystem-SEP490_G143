package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Vietnamese tax code.
 * Vietnam tax codes are exactly 10 or 13 digits only.
 */
public class TaxCodeValidator implements ConstraintValidator<TaxCode, String> {

    // Vietnam tax code: exactly 10 digits OR exactly 13 digits
    private static final String TAX_CODE_REGEX = "^[0-9]{10}$|^[0-9]{13}$";

    @Override
    public void initialize(TaxCode constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String taxCode, ConstraintValidatorContext context) {
        if (taxCode == null || taxCode.trim().isEmpty()) {
            return true; // Optional field - allow null/empty
        }
        return taxCode.trim().matches(TAX_CODE_REGEX);
    }
}
