package com.digital.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class BibliotecaApplicationTests extends IntegracaoBase {

	@Autowired
	ApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(context.getBean(com.digital.biblioteca.config.security.TokenService.class)).isNotNull();
	}

}
