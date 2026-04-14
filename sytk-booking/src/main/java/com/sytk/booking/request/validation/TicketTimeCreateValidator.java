package com.sytk.booking.request.validation;

import com.sytk.booking.request.ConcertCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TicketTimeCreateValidator implements ConstraintValidator<ValidTicketTime, ConcertCreateRequest> {
    @Override
    public boolean isValid(ConcertCreateRequest value, ConstraintValidatorContext context) {
        if (value.ticketOpenAt() == null || value.ticketCloseAt() == null) {
            return true;
        }

        return value.ticketOpenAt().isBefore(value.ticketCloseAt());
    }
}
