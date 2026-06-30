package com.digital.biblioteca.service;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.response.BaixarResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface SupabaseService {

    void subir(MultipartFile conteudo, BucketSupabase bucket, String nome) throws IOException;

    BaixarResponse baixar(String link, BucketSupabase bucket);

    void deletar(String nomeArquivo, BucketSupabase bucket);

}
