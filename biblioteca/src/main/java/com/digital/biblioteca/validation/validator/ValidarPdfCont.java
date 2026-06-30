package com.digital.biblioteca.validation.validator;

import com.digital.biblioteca.validation.annotation.PdfCont;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class ValidarPdfCont
        implements ConstraintValidator<PdfCont, MultipartFile> {

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

        boolean extensaoValida =
                nome.toLowerCase().endsWith(".pdf");

        boolean mimeValido =
                tipo.toLowerCase().contains("pdf");

        return extensaoValida && mimeValido;

    }
}
