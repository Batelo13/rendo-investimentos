package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;

/**
 * O CPF nunca e gravado em claro: so o HMAC-SHA256 com chave fora do banco.
 * Basta para impedir CPF duplicado, e um vazamento do banco nao revela CPFs
 * (hash sem chave seria revertido por forca bruta: sao so ~10^9 CPFs validos).
 * Na inicializacao, converte CPFs antigos ainda gravados em claro (11 digitos).
 */
@Component
public class ProtecaoCpf implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProtecaoCpf.class);

    private final SecretKeySpec chave;
    private final UsuarioRepository usuarioRepository;

    public ProtecaoCpf(@Value("${app.seguranca.cpf-hash-key}") String chave, UsuarioRepository usuarioRepository) {
        if (chave == null || chave.length() < 32) {
            throw new IllegalStateException("CPF_HASH_KEY precisa ter pelo menos 32 caracteres");
        }
        this.chave = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.usuarioRepository = usuarioRepository;
    }

    public String resumo(String cpf) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chave);
            return HexFormat.of().formatHex(mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponivel", e);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Usuario> emClaro = usuarioRepository.findAll().stream()
                .filter(u -> u.getCpf() != null && u.getCpf().matches("\\d{11}"))
                .toList();
        emClaro.forEach(u -> u.setCpf(resumo(u.getCpf())));
        usuarioRepository.saveAll(emClaro);
        if (!emClaro.isEmpty()) {
            log.info("CPF protegido: {} cadastro(s) antigo(s) convertido(s) para HMAC", emClaro.size());
        }
    }
}
