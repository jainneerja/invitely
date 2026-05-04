package com.invitely.repository;

import com.invitely.model.RsvpResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RsvpRepository extends JpaRepository<RsvpResponse, UUID> {

    List<RsvpResponse> findByInvitationIdOrderByCreatedAtDesc(UUID inviteId);

    long countByInvitationId(UUID inviteId);

    @Query("""
        SELECT r.status, COUNT(r), COALESCE(SUM(r.guestCount), 0)
        FROM RsvpResponse r
        WHERE r.invitation.id = :inviteId
        GROUP BY r.status
    """)
    List<Object[]> getRsvpSummaryRaw(UUID inviteId);

    boolean existsByInvitationIdAndGuestEmail(UUID inviteId, String guestEmail);
}
