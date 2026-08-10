package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CorretoraRequestDTO;
import com.curso.gestaoinvestimentos.dto.CorretoraResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.integration.CepClient;
import com.curso.gestaoinvestimentos.integration.CnpjClient;
import com.curso.gestaoinvestimentos.integration.CvmValidador;
import com.curso.gestaoinvestimentos.integration.DadosCepResponse;
import com.curso.gestaoinvestimentos.integration.DadosCnpjResponse;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CorretoraService {

    private final CorretoraRepository repository;
    private final CnpjClient cnpjClient;
    private final CepClient cepClient;

    public CorretoraService(CorretoraRepository repository, CnpjClient cnpjClient, CepClient cepClient) {
        this.repository = repository;
        this.cnpjClient = cnpjClient;
        this.cepClient = cepClient;
    }

    public CorretoraResponseDTO criar(CorretoraRequestDTO dto) {
        repository.findByCnpj(dto.cnpj()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe uma corretora cadastrada com o CNPJ " + dto.cnpj());
        });

        // Isolamento do servico de terceiro (Adapter): o Service so conhece CnpjClient/CepClient,
        // nunca sabe que por tras existe BrasilAPI ou ViaCEP.
        DadosCnpjResponse dadosCnpj = cnpjClient.buscar(dto.cnpj());

        if (dadosCnpj.razaoSocial() == null || dadosCnpj.razaoSocial().isBlank()) {
            throw new RegraDeNegocioException("CNPJ sem razao social na base consultada");
        }
        if (dadosCnpj.situacaoCadastral() != null
                && !dadosCnpj.situacaoCadastral().equalsIgnoreCase("ATIVA")) {
            throw new RegraDeNegocioException(
                    "Instituicao com situacao cadastral nao ativa: " + dadosCnpj.situacaoCadastral());
        }
        if (!CvmValidador.instituicaoValidaNoMercado(dadosCnpj)) {
            throw new RegraDeNegocioException(
                    "Instituicao nao identificada como participante valido do mercado financeiro (CNAE/CVM)");
        }

        DadosCepResponse dadosCep = cepClient.buscar(dadosCnpj.cep());

        Corretora corretora = new Corretora();
        corretora.setCnpj(dadosCnpj.cnpj());
        corretora.setRazaoSocial(dadosCnpj.razaoSocial());
        corretora.setNomeFantasia(dadosCnpj.nomeFantasia());
        corretora.setTelefone(dadosCnpj.telefone());
        corretora.setNumero(dadosCnpj.numero());
        corretora.setComplemento(dadosCnpj.complemento());
        corretora.setSituacaoCadastral(dadosCnpj.situacaoCadastral());

        // Endereco resolvido pelo ViaCEP (fonte mais confiavel para logradouro/bairro/cidade/uf).
        corretora.setCep(dadosCep.cep());
        corretora.setLogradouro(dadosCep.logradouro());
        corretora.setBairro(dadosCep.bairro());
        corretora.setCidade(dadosCep.cidade());
        corretora.setUf(dadosCep.uf());

        // Campo controlado pelo sistema, nunca pelo cliente. So chega aqui se passou pelas
        // validacoes acima (situacao cadastral ativa + CNAE compativel com o mercado financeiro).
        corretora.setValidadaNaCvm(true);
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora nao encontrada com id " + id));
        return toResponseDTO(corretora);
    }

    public CorretoraResponseDTO buscarPorCnpj(String cnpj) {
        Corretora corretora = repository.findByCnpj(cnpj)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora nao encontrada com CNPJ " + cnpj));
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
