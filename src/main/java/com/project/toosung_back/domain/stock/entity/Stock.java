package com.project.toosung_back.domain.stock.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "market", nullable = false, length = 20)
    private String market;

    @Column(name = "country", nullable = false, length = 10)
    private String country;

    @Column(name = "dart_corp_code", length = 8)
    private String dartCorpCode;

    @Column(name = "edgar_cik", length = 10)
    private String edgarCik;

    public void updateDartCorpCode(String dartCorpCode) {
        this.dartCorpCode = dartCorpCode;
    }

    public void updateEdgarCik(String edgarCik) {
        this.edgarCik = edgarCik;
    }
}
