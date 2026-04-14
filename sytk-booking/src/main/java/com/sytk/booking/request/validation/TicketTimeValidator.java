package com.sytk.booking.request.validation;

import com.sytk.booking.request.ConcertEditRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TicketTimeValidator implements ConstraintValidator<ValidTicketTime, ConcertEditRequest> {
    @Override
    public boolean isValid(ConcertEditRequest value, ConstraintValidatorContext context) {
        if (value.ticketOpenAt() == null || value.ticketCloseAt() == null) {
            return true;
        }

        return value.ticketOpenAt().isBefore(value.ticketCloseAt());
    }
}
