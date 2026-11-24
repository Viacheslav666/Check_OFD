package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import ru.ofd.marking.domain.ProductGroup;

@Entity
@Table(name = "nomenclature_rule")
public class NomenclatureRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_group", nullable = false, length = 32)
    private ProductGroup productGroup;
    @Column(nullable = false, columnDefinition = "text")
    private String pattern;
    @Column(nullable = false)
    private int priority = 100;
    @Column(nullable = false)
    private boolean enabled = true;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductGroup getProductGroup() { return productGroup; }
    public void setProductGroup(ProductGroup productGroup) { this.productGroup = productGroup; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
