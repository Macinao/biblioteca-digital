package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.enums.BucketSupabase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@AllArgsConstructor
public class SubirEvent {

    MultipartFile conteudo;

    BucketSupabase bucket;

    String nome;

}
