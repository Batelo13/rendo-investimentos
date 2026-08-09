package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OperacaoService {

    private final OperacaoRepository operacaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final UsuarioRepository usuarioRepository;

    public OperacaoService(OperacaoRepository operacaoRepository, CarteiraRepository carteiraRepository,
                            AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            UsuarioRepository usuarioRepository) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public OperacaoResponseDTO registrar(String emailUsuarioAutenticado, OperacaoRequestDTO dto) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));

        Acao acao = acaoRepository.findById(dto.acaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + dto.acaoId()));
        Corretora corretora = corretoraRepository.findById(dto.corretoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora nao encontrada com id " + dto.corretoraId()));

        if (!Boolean.TRUE.equals(corretora.getValidadaNaCvm())) {
            throw new RegraDeNegocioException("Corretora " + corretora.getNomeFantasia() + " nao e validada na CVM");
        }

        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                carteira.getId(), acao.getId(), corretora.getId(), StatusOperacao.ATIVA);
        PosicaoCalculator.Posicao posicaoAntes = PosicaoCalculator.calcular(historico);

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setCorretora(corretora);
        operacao.setTipo(dto.tipo());
        operacao.setQuantidade(dto.quantidade());
        operacao.setPrecoUnitario(dto.precoUnitario());
        operacao.setDataHora(LocalDateTime.now());
        operacao.setStatus(StatusOperacao.ATIVA);

        if (dto.tipo() == TipoOperacao.VENDA) {
            if (dto.quantidade() > posicaoAntes.quantidade()) {
                throw new RegraDeNegocioException(
                        "Saldo insuficiente: ha " + posicaoAntes.quantidade() + " unidade(s) de "
                                + acao.getTicker() + " nessa corretora, tentando vender " + dto.quantidade());
            }
            operacao.setPrecoMedioNaVenda(posicaoAntes.precoMedio());
        }

        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    public List<OperacaoResponseDTO> listarProprias(String emailUsuarioAutenticado) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));
        return listarPorCarteira(carteira);
    }

    public List<OperacaoResponseDTO> listarComoAdmin(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));
        return listarPorCarteira(carteira);
    }

    @Transactional
    public OperacaoResponseDTO cancelar(Long operacaoId, String emailAdmin) {
        Usuario admin = buscarUsuarioPorEmail(emailAdmin);

        Operacao operacao = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operacao nao encontrada com id " + operacaoId));

        if (operacao.getTipo() != TipoOperacao.COMPRA) {
            throw new RegraDeNegocioException("Apenas operacoes de COMPRA podem ser canceladas");
        }
        if (operacao.getStatus() == StatusOperacao.CANCELADA) {
            throw new RegraDeNegocioException("Operacao " + operacaoId + " ja esta cancelada");
        }

        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                operacao.getCarteira().getId(), operacao.getAcao().getId(), operacao.getCorretora().getId(), StatusOperacao.ATIVA);

        List<Operacao> historicoSemEssaCompra = new ArrayList<>(historico);
        historicoSemEssaCompra.removeIf(op -> op.getId().equals(operacao.getId()));

        PosicaoCalculator.Posicao simulacao = PosicaoCalculator.calcular(historicoSemEssaCompra);
        if (simulacao.quantidadeMinimaHistorica() < 0) {
            throw new RegraDeNegocioException(
                    "Cancelar essa compra deixaria o saldo negativo em alguma venda posterior");
        }

        operacao.setStatus(StatusOperacao.CANCELADA);
        operacao.setCanceladaEm(LocalDateTime.now());
        operacao.setCanceladaPor(admin);

        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    private List<OperacaoResponseDTO> listarPorCarteira(Carteira carteira) {
        return operacaoRepository.findByCarteiraIdOrderByDataHoraDesc(carteira.getId()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + email));
    }

    private OperacaoResponseDTO toResponseDTO(Operacao operacao) {
        BigDecimal lucroPrejuizoRealizado = null;
        if (operacao.getTipo() == TipoOperacao.VENDA && operacao.getPrecoMedioNaVenda() != null) {
            lucroPrejuizoRealizado = operacao.getPrecoUnitario()
                    .subtract(operacao.getPrecoMedioNaVenda())
                    .multiply(BigDecimal.valueOf(operacao.getQuantidade()));
        }

        return new OperacaoResponseDTO(
                operacao.getId(),
                operacao.getTipo(),
                operacao.getQuantidade(),
                operacao.getPrecoUnitario(),
                operacao.getDataHora(),
                operacao.getStatus(),
                operacao.getAcao().getTicker(),
                operacao.getCorretora().getNomeFantasia(),
                operacao.getPrecoMedioNaVenda(),
                lucroPrejuizoRealizado
        );
    }
}
