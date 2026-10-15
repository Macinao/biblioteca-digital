package com.digital.smartteca.storage;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;

/**
 * @EnableRabbit e a exclusao abaixo seguem a mesma logica ja encontrada
 * no search-service com o Kafka: nesta versao do Spring Boot a
 * autoconfiguracao de mensageria/seguranca as vezes nao se ativa sozinha.
 */
@EnableRabbit
@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
public class StorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageServiceApplication.class, args);
    }

}
