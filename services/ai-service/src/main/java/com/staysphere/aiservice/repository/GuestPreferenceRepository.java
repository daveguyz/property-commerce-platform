package com.staysphere.aiservice.repository;
import com.staysphere.aiservice.model.GuestPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GuestPreferenceRepository extends JpaRepository<GuestPreference, String> {
    Optional<GuestPreference> findByGuestId(String guestId);
}
