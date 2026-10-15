package com.digital.biblioteca.validation.annotation;

import com.digital.biblioteca.validation.validator.ValidarPdfCont;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidarPdfCont.class)
public @interface PdfCont {

    String message() default "Arquivo deve ser um PDF válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}