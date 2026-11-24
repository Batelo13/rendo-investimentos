package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.EmailVerificationCodeRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.security.AdministradoresConfigurados;
import com.curso.gestaoinvestimentos.service.UsuarioService;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Papel ADMIN so para emails listados na configuracao do servidor (ADMIN_EMAILS). */
@SpringBootTest(properties = "app.admin.emails= Chefe@Example.com , outra.chefe@example.com")
class AdministradoresConfiguradosIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private EmailVerificationCodeRepository codigoRepository;
    @Autowired private AdministradoresConfigurados administradores;

    @AfterEach
    void limpar() {
        carteiraRepository.deleteAll();
        codigoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Role cadastrar(String email) {
        usuarioService.cadastrar(new UsuarioRequestDTO("Teste", email, CpfTestFixtures.proximoCpfValido(), "Senha1234"));
        return usuarioRepository.findByEmail(email).orElseThrow().getRole();
    }

    @Test
    @DisplayName("Scenario: Email configurado como administrador")
    void emailConfiguradoRecebeAdmin() {
        assertThat(cadastrar("chefe@example.com")).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Scenario: Email comum")
    void emailComumRecebeUser() {
        assertThat(cadastrar("comum@example.com")).isEqualTo(Role.USER);
    }

    @Test
    void usuarioJaExistenteEPromovidoNaInicializacao() {
        Usuario antigo = new Usuario();
        antigo.setNome("Antiga");
        antigo.setEmail("outra.chefe@example.com");
        antigo.setCpf(CpfTestFixtures.proximoCpfValido());
        antigo.setSenha("x");
        antigo.setRole(Role.USER);
        antigo.setAtivo(true);
        antigo.setDataCadastro(LocalDate.now());
        usuarioRepository.save(antigo);

        administradores.run(null);

        assertThat(usuarioRepository.findByEmail("outra.chefe@example.com").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
