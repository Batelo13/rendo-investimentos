package com.curso.gestaoinvestimentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operacoes")
public class Operacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOperacao tipo;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    private BigDecimal precoMedioNaVenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOperacao status;

    private LocalDateTime canceladaEm;

    @ManyToOne
    @JoinColumn(name = "cancelada_por_id")
    private Usuario canceladaPor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public void setCarteira(Carteira carteira) {
        this.carteira = carteira;
    }

    public Acao getAcao() {
        return acao;
    }

    public void setAcao(Acao acao) {
        this.acao = acao;
    }

    public Corretora getCorretora() {
        return corretora;
    }

    public void setCorretora(Corretora corretora) {
        this.corretora = corretora;
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public BigDecimal getPrecoMedioNaVenda() {
        return precoMedioNaVenda;
    }

    public void setPrecoMedioNaVenda(BigDecimal precoMedioNaVenda) {
        this.precoMedioNaVenda = precoMedioNaVenda;
    }

    public StatusOperacao getStatus() {
        return status;
    }

    public void setStatus(StatusOperacao status) {
        this.status = status;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public void setCanceladaEm(LocalDateTime canceladaEm) {
        this.canceladaEm = canceladaEm;
    }

    public Usuario getCanceladaPor() {
        return canceladaPor;
    }

    public void setCanceladaPor(Usuario canceladaPor) {
        this.canceladaPor = canceladaPor;
    }
}
