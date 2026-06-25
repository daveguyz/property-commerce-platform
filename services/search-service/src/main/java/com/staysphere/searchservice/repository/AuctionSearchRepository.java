package com.staysphere.searchservice.repository;

import com.staysphere.searchservice.model.AuctionSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface AuctionSearchRepository extends ElasticsearchRepository<AuctionSearchDocument, String> {

    List<AuctionSearchDocument> findByStatusOrderByStartsAtAsc(String status);

    List<AuctionSearchDocument> findByStatusInOrderByStartsAtAsc(List<String> statuses);

    List<AuctionSearchDocument> findByCityAndStatusIn(String city, List<String> statuses);

    List<AuctionSearchDocument> findByAuctionTypeAndStatusIn(String auctionType, List<String> statuses);
}
