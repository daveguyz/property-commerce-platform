package com.staysphere.messagingservice.repository;
import com.staysphere.messagingservice.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    Page<Message> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);
    @Modifying @Query("UPDATE Message m SET m.read=true, m.readAt=CURRENT_TIMESTAMP WHERE m.conversationId=:convId AND m.senderId != :userId AND m.read=false")
    int markAllAsRead(@Param("convId") String conversationId, @Param("userId") String userId);
    Long countByConversationIdAndReadFalseAndSenderIdNot(String conversationId, String userId);
}
