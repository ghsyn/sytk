package com.sytk.booking.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TicketTimeEditValidator.class, TicketTimeCreateValidator.class})
@Documented
public @interface ValidTicketTime {
    String message() default "티켓 오픈 시간은 마감 시간보다 빨라야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
