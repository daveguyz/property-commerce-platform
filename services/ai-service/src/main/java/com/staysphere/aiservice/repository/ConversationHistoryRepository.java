package com.staysphere.aiservice.repository;
import com.staysphere.aiservice.model.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, String> {
    List<ConversationHistory> findTop10ByGuestIdOrderByCreatedAtDesc(String guestId);
    List<ConversationHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
