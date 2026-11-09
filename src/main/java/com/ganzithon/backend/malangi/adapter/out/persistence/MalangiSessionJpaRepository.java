package com.ganzithon.backend.malangi.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MalangiSessionJpaRepository extends JpaRepository<MalangiSessionEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MalangiSessionEntity s where s.id = :id")
    Optional<MalangiSessionEntity> findByIdForUpdate(@Param("id") long id);
    List<MalangiSessionEntity> findAllByUserIdOrderByStartedAtDesc(long userId, Pageable pageable);
}
