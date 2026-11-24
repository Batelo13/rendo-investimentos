package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.EmailVerificationCodeRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.service.UsuarioService;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug visto rodando o sistema: clique duplo em "Criar conta" mandou dois
 * cadastros iguais ao mesmo tempo; os dois passaram pela checagem de
 * duplicidade e o segundo estourou no banco como erro 500. O resultado
 * correto e: exatamente um cadastro, e toda outra tentativa vira 409.
 */
@SpringBootTest
class CadastroConcorrenteIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private EmailVerificationCodeRepository codigoRepository;

    @AfterEach
    void limpar() {
        codigoRepository.deleteAll();
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void cadastrosSimultaneosIguaisGeramUmUsuarioEConflitoNosDemais() throws Exception {
        for (int rodada = 0; rodada < 5; rodada++) {
            String email = "duplo" + rodada + "@example.com";
            UsuarioRequestDTO dto = new UsuarioRequestDTO("Duplo", email, CpfTestFixtures.proximoCpfValido(), "Senha1234");
            int simultaneos = 4;
            CountDownLatch largada = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(simultaneos);
            List<Future<?>> tentativas = new ArrayList<>();
            for (int i = 0; i < simultaneos; i++) {
                tentativas.add(pool.submit(() -> {
                    largada.await();
                    return usuarioService.cadastrar(dto);
                }));
            }
            largada.countDown();

            int sucessos = 0;
            for (Future<?> tentativa : tentativas) {
                try {
                    tentativa.get();
                    sucessos++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause()).as("falha deve ser conflito (409), nao erro interno")
                            .isInstanceOf(RecursoDuplicadoException.class);
                }
            }
            pool.shutdown();

            assertThat(sucessos).isEqualTo(1);
            assertThat(usuarioRepository.findByEmail(email)).isPresent();
        }
    }
}
