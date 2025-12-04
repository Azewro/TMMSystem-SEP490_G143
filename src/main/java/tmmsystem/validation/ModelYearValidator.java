package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for model year.
 * Must be 4 digits (regex: ^\d{4}$), between 1900 and current year + 1.
 */
public class ModelYearValidator implements ConstraintValidator<ModelYear, Integer> {
    
    private static final int MIN_YEAR = 1900;
    
    @Override
    public void initialize(ModelYear constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        if (year == null) {
            return false;
        }
        
        int currentYear = java.time.Year.now().getValue();
        int maxYear = currentYear + 1;
        
        return year >= MIN_YEAR && year <= maxYear;
    }
}

