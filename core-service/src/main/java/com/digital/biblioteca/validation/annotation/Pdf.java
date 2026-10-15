package com.digital.biblioteca.validation.annotation;

import com.digital.biblioteca.validation.validator.ValidarPdf;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidarPdf.class)
public @interface Pdf {

    String message() default "Arquivo deve ser um PDF válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}