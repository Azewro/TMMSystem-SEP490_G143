package tmmsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/**
 * Validator for expected delivery date.
 * Must be at least 30 days from today.
 * Matches frontend: CreateRfqPage.jsx - getMinExpectedDeliveryDate
 */
public class ExpectedDeliveryDateValidator implements ConstraintValidator<ExpectedDeliveryDate, LocalDate> {
    
    private static final int MIN_DAYS_FROM_TODAY = 30;
    
    @Override
    public void initialize(ExpectedDeliveryDate constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(LocalDate expectedDeliveryDate, ConstraintValidatorContext context) {
        if (expectedDeliveryDate == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.plusDays(MIN_DAYS_FROM_TODAY);
        
        return !expectedDeliveryDate.isBefore(minDate);
    }
}

