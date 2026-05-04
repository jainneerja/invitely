package com.invitely.repository;

import com.invitely.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT i FROM Invitation i LEFT JOIN FETCH i.assets WHERE i.slug = :slug")
    Optional<Invitation> findBySlugWithAssets(String slug);

    @Query("SELECT i FROM Invitation i LEFT JOIN FETCH i.template WHERE i.id = :id")
    Optional<Invitation> findByIdWithTemplate(UUID id);
}
