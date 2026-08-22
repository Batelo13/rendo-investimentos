package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.dto.RendimentoPontoDTO;
import com.curso.gestaoinvestimentos.dto.SaldoDTO;
import com.curso.gestaoinvestimentos.service.CarteiraService;
import com.curso.gestaoinvestimentos.service.OperacaoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/carteiras")
public class CarteiraController {

    private final CarteiraService carteiraService;
    private final OperacaoService operacaoService;

    public CarteiraController(CarteiraService carteiraService, OperacaoService operacaoService) {
        this.carteiraService = carteiraService;
        this.operacaoService = operacaoService;
    }

    @GetMapping("/me")
    public List<PosicaoDTO> posicaoPropria(Principal principal) {
        return carteiraService.buscarPosicaoPropria(principal.getName());
    }

    @GetMapping("/me/operacoes")
    public Page<OperacaoResponseDTO> operacoesProprias(Principal principal, @PageableDefault(size = 20) Pageable pageable) {
        return operacaoService.listarProprias(principal.getName(), pageable);
    }

    @GetMapping("/me/saldo")
    public SaldoDTO saldoProprio(Principal principal) {
        return carteiraService.buscarSaldoPropria(principal.getName());
    }

    @GetMapping("/me/rendimento-historico")
    public List<RendimentoPontoDTO> rendimentoProprio(Principal principal) {
        return carteiraService.buscarRendimentoPropria(principal.getName());
    }

    @GetMapping("/{usuarioId}")
    public List<PosicaoDTO> posicaoPorUsuario(@PathVariable Long usuarioId) {
        return carteiraService.buscarPosicaoPorUsuarioId(usuarioId);
    }

    @GetMapping("/{usuarioId}/operacoes")
    public Page<OperacaoResponseDTO> operacoesPorUsuario(@PathVariable Long usuarioId, @PageableDefault(size = 20) Pageable pageable) {
        return operacaoService.listarComoAdmin(usuarioId, pageable);
    }

    @PatchMapping("/{usuarioId}/reconstruir")
    public List<PosicaoDTO> reconstruir(@PathVariable Long usuarioId) {
        return carteiraService.reconstruirPosicao(usuarioId);
    }
}
