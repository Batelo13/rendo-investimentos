package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CorretoraRequestDTO;
import com.curso.gestaoinvestimentos.dto.CorretoraResponseDTO;
import com.curso.gestaoinvestimentos.exception.CorretoraDuplicadaException;
import com.curso.gestaoinvestimentos.exception.CorretoraNaoEncontradaException;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CorretoraService {

    private final CorretoraRepository repository;

    public CorretoraService(CorretoraRepository repository) {
        this.repository = repository;
    }

    public CorretoraResponseDTO criar(CorretoraRequestDTO dto) {
        repository.findByCnpj(dto.cnpj()).ifPresent(existente -> {
            throw new CorretoraDuplicadaException("Ja existe uma corretora cadastrada com o CNPJ " + dto.cnpj());
        });

        Corretora corretora = new Corretora();
        corretora.setCnpj(dto.cnpj());
        corretora.setRazaoSocial(dto.razaoSocial());
        corretora.setNomeFantasia(dto.nomeFantasia());
        corretora.setEmail(dto.email());
        corretora.setTelefone(dto.telefone());
        corretora.setCep(dto.cep());
        corretora.setLogradouro(dto.logradouro());
        corretora.setNumero(dto.numero());
        corretora.setComplemento(dto.complemento());
        corretora.setBairro(dto.bairro());
        corretora.setCidade(dto.cidade());
        corretora.setUf(dto.uf());

        // Campos controlados pelo sistema, nunca pelo cliente:
        corretora.setSituacaoCadastral("PENDENTE");
        corretora.setValidadaNaCvm(false);
        corretora.setDataCadastro(LocalDate.now());

        Corretora salva = repository.save(corretora);
        return toResponseDTO(salva);
    }

    public List<CorretoraResponseDTO> listar() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CorretoraResponseDTO buscarPorId(Long id) {
        Corretora corretora = repository.findById(id)
                .orElseThrow(() -> new CorretoraNaoEncontradaException("Corretora nao encontrada com id " + id));
        return toResponseDTO(corretora);
    }

    public CorretoraResponseDTO buscarPorCnpj(String cnpj) {
        Corretora corretora = repository.findByCnpj(cnpj)
                .orElseThrow(() -> new CorretoraNaoEncontradaException("Corretora nao encontrada com CNPJ " + cnpj));
        return toResponseDTO(corretora);
    }

    private CorretoraResponseDTO toResponseDTO(Corretora corretora) {
        return new CorretoraResponseDTO(
                corretora.getId(),
                corretora.getCnpj(),
                corretora.getRazaoSocial(),
                corretora.getNomeFantasia(),
                corretora.getEmail(),
                corretora.getTelefone(),
                corretora.getCep(),
                corretora.getLogradouro(),
                corretora.getNumero(),
                corretora.getComplemento(),
                corretora.getBairro(),
                corretora.getCidade(),
                corretora.getUf(),
                corretora.getSituacaoCadastral(),
                corretora.getValidadaNaCvm(),
                corretora.getDataCadastro()
        );
    }
}
