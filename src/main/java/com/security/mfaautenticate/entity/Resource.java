package com.security.mfaautenticate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String path;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL)
    private Set<Permission> permissions = new HashSet<>();
}
