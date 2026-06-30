package com.digital.biblioteca.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CadastrarUsuarioRequest {

        @NotBlank
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        @Pattern(
                regexp = "^[A-Za-zÀ-ÿ' -]+$",
                message = "Nome só pode conter letras, espaços, apóstrofo e hífen"
        )
        String nome;

        @NotBlank
        @Size(min = 6, max = 20, message = "Matrícula deve ter entre 6 e 20 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$",
                message = "Matrícula só pode conter letras e números"
        )
        String matricula;

        @NotBlank
        @Size(min = 8, max = 64, message = "Senha deve ter entre 8 e 64 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
                message = "Senha deve conter ao menos uma letra maiúscula, uma minúscula e um número"
        )
        String senha;

        @NotBlank
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 150, message = "E-mail muito longo")
        String email;

        @NotBlank
        @Pattern(
                regexp = "^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$",
                message = "Telefone inválido. Use formato (00) 00000-0000 ou 00000000000"
        )
        String telefone;

        @NotBlank
        @Pattern(
                regexp = "^(ALUNO|PROFESSOR|BIBLIOTECARIO)$",
                message = "Role deve ser ALUNO, PROFESSOR ou BIBLIOTECARIO"
        )
        String role;

}
