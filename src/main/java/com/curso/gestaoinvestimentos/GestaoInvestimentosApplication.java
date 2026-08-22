package com.curso.gestaoinvestimentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GestaoInvestimentosApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoInvestimentosApplication.class, args);
    }

}
