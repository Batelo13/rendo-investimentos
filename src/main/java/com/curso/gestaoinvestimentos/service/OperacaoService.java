package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.TransferenciaRequestDTO;
import com.curso.gestaoinvestimentos.dto.TransferenciaResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.integration.DolarApiCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final PosicaoCacheService posicaoCacheService;
    private final DolarApiCambioClient cambioClient;

    public OperacaoService(OperacaoRepository operacaoRepository, CarteiraRepository carteiraRepository,
                            AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            UsuarioRepository usuarioRepository, PosicaoCacheService posicaoCacheService,
                            DolarApiCambioClient cambioClient) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
        this.cambioClient = cambioClient;
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

        validarQuantidadeInteiraSeBrasil(acao, dto.quantidade());

        BigDecimal taxaCambio = acao.getMercado() == Mercado.EUA
                ? cambioClient.buscarTaxaUsdParaBrl()
                : BigDecimal.ONE;

        if (dto.tipo() == TipoOperacao.COMPRA) {
            List<Operacao> historicoCarteira = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(
                    carteira.getId(), StatusOperacao.ATIVA);
            BigDecimal saldoDisponivel = SaldoCalculator.calcular(carteira.getSaldoInicial(), historicoCarteira);
            BigDecimal custoCompra = dto.precoUnitario().multiply(dto.quantidade()).multiply(taxaCambio);
            if (custoCompra.compareTo(saldoDisponivel) > 0) {
                throw new RegraDeNegocioException(
                        "Saldo em conta insuficiente: disponivel R$ " + saldoDisponivel
                                + ", tentando comprar R$ " + custoCompra);
            }
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
        operacao.setTaxaCambio(taxaCambio);
        operacao.setDataHora(LocalDateTime.now());
        operacao.setStatus(StatusOperacao.ATIVA);

        if (dto.tipo() == TipoOperacao.VENDA) {
            if (dto.quantidade().compareTo(posicaoAntes.quantidade()) > 0) {
                throw new RegraDeNegocioException(
                        "Saldo insuficiente: ha " + posicaoAntes.quantidade() + " unidade(s) de "
                                + acao.getTicker() + " nessa corretora, tentando vender " + dto.quantidade());
            }
            operacao.setPrecoMedioNaVenda(posicaoAntes.precoMedio());
        }

        Operacao salva = operacaoRepository.save(operacao);
        posicaoCacheService.atualizar(carteira, acao, corretora);
        return toResponseDTO(salva);
    }

    public Page<OperacaoResponseDTO> listarProprias(String emailUsuarioAutenticado, Pageable pageable) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));
        return listarPorCarteira(carteira, pageable);
    }

    public Page<OperacaoResponseDTO> listarComoAdmin(Long usuarioId, Pageable pageable) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));
        return listarPorCarteira(carteira, pageable);
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
        if (simulacao.quantidadeMinimaHistorica().compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException(
                    "Cancelar essa compra deixaria o saldo negativo em alguma venda posterior");
        }

        operacao.setStatus(StatusOperacao.CANCELADA);
        operacao.setCanceladaEm(LocalDateTime.now());
        operacao.setCanceladaPor(admin);

        Operacao salva = operacaoRepository.save(operacao);
        posicaoCacheService.atualizar(operacao.getCarteira(), operacao.getAcao(), operacao.getCorretora());
        return toResponseDTO(salva);
    }

    @Transactional
    public TransferenciaResponseDTO transferir(String emailUsuarioAutenticado, TransferenciaRequestDTO dto) {
        if (dto.corretoraOrigemId().equals(dto.corretoraDestinoId())) {
            throw new RegraDeNegocioException("Corretora de origem e destino devem ser diferentes");
        }

        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));

        Acao acao = acaoRepository.findById(dto.acaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + dto.acaoId()));
        Corretora origem = corretoraRepository.findById(dto.corretoraOrigemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora de origem nao encontrada com id " + dto.corretoraOrigemId()));
        Corretora destino = corretoraRepository.findById(dto.corretoraDestinoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora de destino nao encontrada com id " + dto.corretoraDestinoId()));

        if (!Boolean.TRUE.equals(destino.getValidadaNaCvm())) {
            throw new RegraDeNegocioException("Corretora " + destino.getNomeFantasia() + " nao e validada na CVM");
        }

        validarQuantidadeInteiraSeBrasil(acao, dto.quantidade());

        List<Operacao> historicoOrigem = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                carteira.getId(), acao.getId(), origem.getId(), StatusOperacao.ATIVA);
        PosicaoCalculator.Posicao posicaoOrigem = PosicaoCalculator.calcular(historicoOrigem);

        if (dto.quantidade().compareTo(posicaoOrigem.quantidade()) > 0) {
            throw new RegraDeNegocioException(
                    "Saldo insuficiente: ha " + posicaoOrigem.quantidade() + " unidade(s) de "
                            + acao.getTicker() + " na corretora de origem, tentando transferir " + dto.quantidade());
        }

        BigDecimal taxaCambio = acao.getMercado() == Mercado.EUA
                ? cambioClient.buscarTaxaUsdParaBrl()
                : BigDecimal.ONE;
        LocalDateTime agora = LocalDateTime.now();

        // Transferencia nao e compra nem venda: preco unitario dos dois lados e o
        // preco medio de origem (sem lucro/prejuizo artificial) e nao mexe no
        // saldo em conta (ver SaldoCalculator). Sao dois lancamentos porque uma
        // Operacao so pertence a uma corretora por vez.
        Operacao saida = new Operacao();
        saida.setCarteira(carteira);
        saida.setAcao(acao);
        saida.setCorretora(origem);
        saida.setTipo(TipoOperacao.TRANSFERENCIA_SAIDA);
        saida.setQuantidade(dto.quantidade());
        saida.setPrecoUnitario(posicaoOrigem.precoMedio());
        saida.setTaxaCambio(taxaCambio);
        saida.setDataHora(agora);
        saida.setStatus(StatusOperacao.ATIVA);
        Operacao saidaSalva = operacaoRepository.save(saida);

        Operacao entrada = new Operacao();
        entrada.setCarteira(carteira);
        entrada.setAcao(acao);
        entrada.setCorretora(destino);
        entrada.setTipo(TipoOperacao.TRANSFERENCIA_ENTRADA);
        entrada.setQuantidade(dto.quantidade());
        entrada.setPrecoUnitario(posicaoOrigem.precoMedio());
        entrada.setTaxaCambio(taxaCambio);
        entrada.setDataHora(agora);
        entrada.setStatus(StatusOperacao.ATIVA);
        Operacao entradaSalva = operacaoRepository.save(entrada);

        saidaSalva.setOperacaoVinculada(entradaSalva);
        operacaoRepository.save(saidaSalva);

        posicaoCacheService.atualizar(carteira, acao, origem);
        posicaoCacheService.atualizar(carteira, acao, destino);

        return new TransferenciaResponseDTO(toResponseDTO(saidaSalva), toResponseDTO(entradaSalva));
    }

    private void validarQuantidadeInteiraSeBrasil(Acao acao, BigDecimal quantidade) {
        // Mercado fracionario brasileiro negocia em unidades inteiras (1, 2, 3...);
        // fracao de acao (0,5) so existe nos EUA, onde corretoras vendem "fatias".
        if (acao.getMercado() == Mercado.BRASIL && quantidade.stripTrailingZeros().scale() > 0) {
            throw new RegraDeNegocioException(
                    "Acoes brasileiras nao permitem quantidade fracionaria: " + quantidade);
        }
    }

    private Page<OperacaoResponseDTO> listarPorCarteira(Carteira carteira, Pageable pageable) {
        return operacaoRepository.findByCarteiraIdOrderByDataHoraDesc(carteira.getId(), pageable)
                .map(this::toResponseDTO);
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
                    .multiply(operacao.getQuantidade());
        }

        return new OperacaoResponseDTO(
                operacao.getId(),
                operacao.getTipo(),
                operacao.getQuantidade(),
                operacao.getPrecoUnitario(),
                operacao.getTaxaCambio(),
                operacao.getDataHora(),
                operacao.getStatus(),
                operacao.getAcao().getTicker(),
                operacao.getCorretora().getNomeFantasia(),
                operacao.getPrecoMedioNaVenda(),
                lucroPrejuizoRealizado
        );
    }
}
