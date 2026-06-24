package com.staysphere.pricingengine.repository;
import com.staysphere.pricingengine.model.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, String> {
    List<PricingRule> findByPropertyIdAndEnabledTrueOrderByPriorityDesc(String propertyId);
    List<PricingRule> findByPropertyIdIsNullAndEnabledTrue();
}
