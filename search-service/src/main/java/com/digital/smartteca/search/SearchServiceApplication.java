package com.digital.smartteca.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * @EnableKafka explicito porque, nesta versao do Spring Boot (4.1), a
 * autoconfiguracao nao esta habilitando sozinha o processamento de
 * @KafkaListener — sem isso, os consumers ficam registrados como bean
 * mas nunca se inscrevem de fato nos topicos (confirmado: nenhum
 * consumer group aparecia no broker).
 *
 * UserDetailsServiceAutoConfiguration excluida porque o GatewayHeaderAuthFilter
 * monta a autenticacao direto a partir dos headers do gateway — nunca usamos
 * o AuthenticationManager/UserDetailsService padrao do Spring Security, e sem
 * essa exclusao o Boot cria um usuario em memoria com senha aleatoria a cada
 * subida, so pra logar um warning inutil.
 */
@EnableKafka
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }

}
