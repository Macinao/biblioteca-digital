package com.digital.biblioteca.validation.validator;

import com.digital.biblioteca.validation.annotation.ImagemCont;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public class ValidarImagemCont
        implements ConstraintValidator<ImagemCont, MultipartFile> {

    @Override
    public boolean isValid(
            MultipartFile file,
            ConstraintValidatorContext context
    ) {

        if (file == null || file.isEmpty()) {
            return false;
        }

        String nome = file.getOriginalFilename();
        String tipo = file.getContentType();

        if (nome == null || tipo == null) {
            return false;
        }

        String nomeLower =
                nome.toLowerCase(Locale.ROOT);

        boolean extensaoValida =
                nomeLower.endsWith(".png") ||
                        nomeLower.endsWith(".jpg") ||
                        nomeLower.endsWith(".jpeg") ||
                        nomeLower.endsWith(".webp");

        boolean mimeValido =
                "image/png".equalsIgnoreCase(tipo) ||
                        "image/jpeg".equalsIgnoreCase(tipo) ||
                        "image/webp".equalsIgnoreCase(tipo);

        return extensaoValida && mimeValido;

    }
}
