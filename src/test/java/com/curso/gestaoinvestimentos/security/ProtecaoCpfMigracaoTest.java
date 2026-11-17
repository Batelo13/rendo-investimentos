package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cadastros antigos com CPF em claro sao convertidos para HMAC na inicializacao. */
class ProtecaoCpfMigracaoTest {

    private static final String CHAVE = "chave-de-teste-com-mais-de-32-caracteres";

    private static Usuario comCpf(String cpf) {
        Usuario u = new Usuario();
        u.setCpf(cpf);
        return u;
    }

    @Test
    void converteCpfEmClaroEPreservaOsJaProtegidos() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        ProtecaoCpf protecao = new ProtecaoCpf(CHAVE, repositorio);
        String jaProtegido = protecao.resumo("39053344705");
        Usuario antigo = comCpf("52998224725");
        Usuario novo = comCpf(jaProtegido);
        when(repositorio.findAll()).thenReturn(List.of(antigo, novo));

        protecao.run(null);

        assertThat(antigo.getCpf()).isEqualTo(protecao.resumo("52998224725")).matches("[0-9a-f]{64}");
        assertThat(novo.getCpf()).isEqualTo(jaProtegido);
    }

    @Test
    void mesmoCpfMesmaChaveMesmoResumo_chaveDiferenteResumoDiferente() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        String a = new ProtecaoCpf(CHAVE, repositorio).resumo("52998224725");
        assertThat(new ProtecaoCpf(CHAVE, repositorio).resumo("52998224725")).isEqualTo(a);
        assertThat(new ProtecaoCpf(CHAVE + "x", repositorio).resumo("52998224725")).isNotEqualTo(a);
    }

    @Test
    void recusaChaveCurta() {
        assertThatThrownBy(() -> new ProtecaoCpf("curta", mock(UsuarioRepository.class)))
                .isInstanceOf(IllegalStateException.class);
    }
}
