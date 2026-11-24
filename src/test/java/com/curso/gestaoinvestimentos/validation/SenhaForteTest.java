package com.curso.gestaoinvestimentos.validation;

import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Regra de senha do cadastro: 8+ caracteres, com maiuscula e minuscula (valores-limite). */
class SenhaForteTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        fabrica.close();
    }

    private Set<String> errosDeSenha(String senha) {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Maria", "maria@example.com", "11144477735", senha);
        return validator.validate(dto).stream()
                .filter(v -> v.getPropertyPath().toString().equals("senha"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest(name = "\"{0}\" e aceita")
    @CsvSource({"Senha123", "aB345678", "MinhaSenhaForte", "Çãobcdef"})
    void aceitaSenhaComMaiusculaMinusculaEOitoCaracteres(String senha) {
        assertThat(errosDeSenha(senha)).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "Senha12, Senha deve ter no minimo 8 caracteres",
            "senha1234, Senha deve ter letra maiuscula e minuscula",
            "SENHA1234, Senha deve ter letra maiuscula e minuscula",
            "12345678, Senha deve ter letra maiuscula e minuscula"
    })
    void recusaSenhaFraca(String senha, String mensagem) {
        assertThat(errosDeSenha(senha)).contains(mensagem);
    }
}
