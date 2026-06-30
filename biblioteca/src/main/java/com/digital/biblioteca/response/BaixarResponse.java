package com.digital.biblioteca.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BaixarResponse {

    private Resource resource;

    private String contentType;

    private String nome;

}
