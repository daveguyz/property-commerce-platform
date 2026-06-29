package com.propertycommerce.propertyservice.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "amenities") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Amenity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "property_id") private Property property;
    @Column(nullable = false) private String name;
    private String category, icon, detail;
}
