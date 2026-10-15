package com.digital.biblioteca.validation.annotation;

import com.digital.biblioteca.validation.validator.ValidarImagemCont;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidarImagemCont.class)
public @interface ImagemCont {

    String message() default "Arquivo deve ser uma imagem válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}