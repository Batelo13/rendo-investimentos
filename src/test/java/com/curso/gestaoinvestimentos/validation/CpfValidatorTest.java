package com.curso.gestaoinvestimentos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void aceitaCpfValidoComDigitosVerificadoresCorretos() {
        assertTrue(validator.isValid("11144477735", null));
        assertTrue(validator.isValid("12345678909", null));
    }

    @Test
    void rejeitaCpfComDigitoVerificadorErrado() {
        assertFalse(validator.isValid("11144477736", null));
    }

    @Test
    void rejeitaCpfComTodosOsDigitosIguais() {
        assertFalse(validator.isValid("11111111111", null));
        assertFalse(validator.isValid("00000000000", null));
    }

    @Test
    void rejeitaCpfComTamanhoErradoOuComPontuacao() {
        assertFalse(validator.isValid("111444777", null));
        assertFalse(validator.isValid("111.444.777-35", null));
    }

    @Test
    void aceitaNuloOuVazioDeixandoNotBlankCuidarDaObrigatoriedade() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
    }
}
