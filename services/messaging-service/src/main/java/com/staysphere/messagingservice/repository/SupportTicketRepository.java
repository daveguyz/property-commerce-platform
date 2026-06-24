package com.staysphere.messagingservice.repository;
import com.staysphere.messagingservice.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus status, Pageable pageable);
    List<SupportTicket> findByAssignedAdminIdAndStatusNot(String adminId, SupportTicket.TicketStatus status);
    Long countByStatus(SupportTicket.TicketStatus status);
}
