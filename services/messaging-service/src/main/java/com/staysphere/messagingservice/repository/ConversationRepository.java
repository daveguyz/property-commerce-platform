package com.staysphere.messagingservice.repository;
import com.staysphere.messagingservice.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    @Query("SELECT c FROM Conversation c WHERE (c.participantOneId=:userId OR c.participantTwoId=:userId) ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findByParticipant(@Param("userId") String userId, Pageable pageable);
    Optional<Conversation> findByBookingId(String bookingId);
    @Query("SELECT c FROM Conversation c WHERE (c.participantOneId=:u1 AND c.participantTwoId=:u2) OR (c.participantOneId=:u2 AND c.participantTwoId=:u1) AND c.bookingId IS NULL")
    Optional<Conversation> findDirectConversation(@Param("u1") String user1, @Param("u2") String user2);
    @Query("SELECT SUM(CASE WHEN c.participantOneId=:userId THEN c.unreadCountOne ELSE c.unreadCountTwo END) FROM Conversation c WHERE (c.participantOneId=:userId OR c.participantTwoId=:userId)")
    Integer getTotalUnreadCount(@Param("userId") String userId);
}
