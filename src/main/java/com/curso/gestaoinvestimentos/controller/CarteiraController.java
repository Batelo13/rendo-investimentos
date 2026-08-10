package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.service.CarteiraService;
import com.curso.gestaoinvestimentos.service.OperacaoService;
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
    public List<OperacaoResponseDTO> operacoesProprias(Principal principal) {
        return operacaoService.listarProprias(principal.getName());
    }

    @GetMapping("/{usuarioId}")
    public List<PosicaoDTO> posicaoPorUsuario(@PathVariable Long usuarioId) {
        return carteiraService.buscarPosicaoPorUsuarioId(usuarioId);
    }

    @GetMapping("/{usuarioId}/operacoes")
    public List<OperacaoResponseDTO> operacoesPorUsuario(@PathVariable Long usuarioId) {
        return operacaoService.listarComoAdmin(usuarioId);
    }

    @PatchMapping("/{usuarioId}/reconstruir")
    public List<PosicaoDTO> reconstruir(@PathVariable Long usuarioId) {
        return carteiraService.reconstruirPosicao(usuarioId);
    }
}
