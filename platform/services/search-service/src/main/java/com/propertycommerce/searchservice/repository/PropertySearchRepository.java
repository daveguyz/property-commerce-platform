package com.propertycommerce.searchservice.repository;
import com.propertycommerce.searchservice.model.PropertySearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertySearchRepository extends ElasticsearchRepository<PropertySearchDocument, String> {
    Page<PropertySearchDocument> findByCityAndStatus(String city, String status, Pageable pageable);
    Page<PropertySearchDocument> findByStatusAndBedroomsGreaterThanEqual(String status, int bedrooms, Pageable pageable);
    void deleteByHostId(String hostId);
}
