package com.curso.gestaoinvestimentos.integration;

/**
 * Valida compatibilidade com o mercado financeiro via CNAE da Receita Federal.
 * CNAEs dos grupos 6611/6612/6619 referem-se a corretoras e atividades correlatas
 * (corretagem de valores, cambio, seguros, administracao de mercados financeiros).
 * Fonte publica equivalente a exigencia de validacao CVM do trabalho academico -
 * nao ha API publica gratuita que exponha o registro CVM diretamente.
 */
public class CvmValidador {

    private static final String[] CNAES_FINANCEIROS_PREFIXOS = {
            "6612601", "6612602", "6612603", "6612604", "6612605",
            "6612301", "6612302", "6611801", "6611802", "6611803",
            "6619302", "6619303", "6619304", "6422100", "6423900"
    };

    private CvmValidador() {
    }

    public static boolean instituicaoValidaNoMercado(DadosCnpjResponse dadosCnpj) {
        if (dadosCnpj.cnaePrincipal() == null) {
            return false;
        }
        String cnae = dadosCnpj.cnaePrincipal().replaceAll("\\D", "");
        for (String prefixo : CNAES_FINANCEIROS_PREFIXOS) {
            if (cnae.startsWith(prefixo) || cnae.equals(prefixo)) {
                return true;
            }
        }
        return cnae.startsWith("6612") || cnae.startsWith("6611") || cnae.startsWith("6619");
    }
}
