package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.EmailVerificationCode;
import com.curso.gestaoinvestimentos.model.TipoCodigo;
import com.curso.gestaoinvestimentos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(Usuario usuario, TipoCodigo tipo);

    // Usado no cooldown de reenvio: precisa do ultimo codigo gerado, mesmo se
    // ja tiver sido usado/invalidado, pra medir o intervalo desde a ultima
    // geracao (nao so desde o ultimo codigo ainda ativo).
    Optional<EmailVerificationCode> findFirstByUsuarioAndTipoOrderByCriadoEmDesc(Usuario usuario, TipoCodigo tipo);
}
