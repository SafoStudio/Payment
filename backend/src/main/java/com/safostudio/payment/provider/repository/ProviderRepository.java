package com.safostudio.payment.provider.repository;

import com.safostudio.payment.provider.domain.Provider;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends CrudRepository<Provider, UUID>,
        PagingAndSortingRepository<Provider, UUID> {

  Optional<Provider> findByCode(String code);

  @Query("""
      SELECT * FROM providers
      WHERE (:status::text IS NULL OR status = :status::text)
        AND (:type::text IS NULL OR type = :type::text)
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<Provider> findPage(
          @Param("status") String status,
          @Param("type") String type,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}