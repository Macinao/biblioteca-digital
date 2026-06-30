package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.config.supabase.SupabaseProperties;
import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.response.BaixarResponse;
import com.digital.biblioteca.service.SupabaseService;
import com.digital.biblioteca.treatment.IntegracaoExternaException;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseServiceImpl implements SupabaseService {

    private final SupabaseProperties supabaseProperties;
    private final RestTemplate restTemplate;

    @Override
    public void subir(MultipartFile conteudo, BucketSupabase bucket, String nome) throws IOException {

        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + bucket.getBucket()
                + "/"
                + nome;

        log.debug("Upload Supabase bucket={} arquivo={}", bucket.getBucket(), nome);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getKey());
        headers.set("apikey", supabaseProperties.getKey());
        headers.setContentType(MediaType.valueOf(conteudo.getContentType()));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> request = new HttpEntity<>(conteudo.getBytes(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IntegracaoExternaException(
                    "Falha ao subir arquivo para o Supabase (status: " + response.getStatusCode() + ")");
        }

    }

    @Override
    public BaixarResponse baixar(String link, BucketSupabase bucket) {

        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + bucket.getBucket()
                + "/"
                + link;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getKey());
        headers.set("apikey", supabaseProperties.getKey());

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado no Supabase");
        }

        String contentType = response.getHeaders()
                .getContentType()
                .toString();

        ByteArrayResource resource =
                new ByteArrayResource(response.getBody());

        BaixarResponse arquivo = new BaixarResponse();
        arquivo.setResource(resource);
        arquivo.setContentType(contentType);
        arquivo.setNome(link);

        return arquivo;

    }

    @Override
    public void deletar(String nomeArquivo, BucketSupabase bucket) {

        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + bucket.getBucket()
                + "/"
                + nomeArquivo;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getKey());
        headers.set("apikey", supabaseProperties.getKey());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IntegracaoExternaException(
                    "Falha ao deletar arquivo no Supabase (status: " + response.getStatusCode() + ")");
        }

    }

}
