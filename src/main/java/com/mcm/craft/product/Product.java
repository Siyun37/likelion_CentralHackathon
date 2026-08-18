package com.mcm.craft.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String colors;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sizes;

    @Column(columnDefinition = "TEXT")
    private String heritageText;

    @Column(nullable = false)
    private String launchStatus;

    public Product(String name, Long price, String category, String colors, String sizes,
                    String heritageText, String launchStatus) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.colors = colors;
        this.sizes = sizes;
        this.heritageText = heritageText;
        this.launchStatus = launchStatus;
    }
}
