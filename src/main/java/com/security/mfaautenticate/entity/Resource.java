package com.security.mfaautenticate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa una ENTIDAD DE NEGOCIO del sistema (no una ruta API).
 * Ejemplos: Usuarios, Productos, Documentos, Órdenes, Clientes, etc.
 * Los permisos definen qué operaciones (CREATE, READ, UPDATE, DELETE)
 * se pueden realizar sobre cada recurso.
 */
@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"permissions"})
@ToString(exclude = {"permissions"})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la entidad de negocio.
     * Ejemplos: "Productos", "Usuarios", "Documentos", "Órdenes"
     */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Descripción de qué representa este recurso en el negocio.
     */
    private String description;

    /**
     * Identificador/slug para referencia interna (opcional).
     * Ejemplos: "products", "users", "documents"
     * NO debe ser una ruta API completa como "/api/users"
     */
    @Column(nullable = false)
    private String path;

    @JsonIgnore
    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL)
    private Set<Permission> permissions = new HashSet<>();
}
