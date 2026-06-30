    package com.digital.biblioteca.validation.annotation;

    import com.digital.biblioteca.validation.validator.ValidarImagem;
    import jakarta.validation.Constraint;
    import jakarta.validation.Payload;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    @Target({ ElementType.PARAMETER, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidarImagem.class)
    public @interface Imagem {

        String message() default "Arquivo deve ser uma imagem válido";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }