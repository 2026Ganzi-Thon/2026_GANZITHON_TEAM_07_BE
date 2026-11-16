package com.ganzithon.backend.counseling.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CounselingSessionJpaRepository extends JpaRepository<CounselingSessionEntity, Long> {
    @Query("select s from CounselingSessionEntity s where s.id = :id and s.userId = :userId")
    Optional<CounselingSessionEntity> findOwned(@Param("id") long id, @Param("userId") long userId);

    @Modifying
    @Query("delete from CounselingSessionEntity s where s.id = :id and s.userId = :userId")
    int deleteOwned(@Param("id") long id, @Param("userId") long userId);
}

interface CounselingMessageJpaRepository extends JpaRepository<CounselingMessageEntity, Long> {
    @Query("select m from CounselingMessageEntity m where m.sessionId = :sessionId order by m.id desc")
    List<CounselingMessageEntity> findRecent(@Param("sessionId") long sessionId, Pageable pageable);

    @Query("select m from CounselingMessageEntity m where m.sessionId = :sessionId order by m.id asc")
    List<CounselingMessageEntity> findAllBySession(@Param("sessionId") long sessionId);

    @Modifying
    @Query("delete from CounselingMessageEntity m where m.sessionId = :sessionId")
    int deleteBySession(@Param("sessionId") long sessionId);
}

interface CounselingResultJpaRepository extends JpaRepository<CounselingResultEntity, Long> {
    @Query("select r from CounselingResultEntity r where r.userId = :userId and r.createdAt >= :from and r.createdAt < :to "
            + "order by r.createdAt desc, r.id desc")
    List<CounselingResultEntity> findPage(@Param("userId") long userId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select count(r) from CounselingResultEntity r where r.userId = :userId and r.createdAt >= :from and r.createdAt < :to")
    long countByUser(@Param("userId") long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select r from CounselingResultEntity r where r.userId = :userId and r.createdAt >= :from and r.createdAt < :to "
            + "order by r.createdAt asc, r.id asc")
    List<CounselingResultEntity> findAllBetween(@Param("userId") long userId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("select r from CounselingResultEntity r where r.id = :id and r.userId = :userId")
    Optional<CounselingResultEntity> findOwned(@Param("id") long id, @Param("userId") long userId);

    @Modifying
    @Query("delete from CounselingResultEntity r where r.sessionId = :sessionId")
    int deleteBySession(@Param("sessionId") long sessionId);
}

interface CounselingReportJpaRepository extends JpaRepository<CounselingReportEntity, Long> {
    @Query("select r from CounselingReportEntity r where r.userId = :userId and r.periodYear = :year and r.periodMonth = :month")
    Optional<CounselingReportEntity> findPeriod(@Param("userId") long userId, @Param("year") int year,
            @Param("month") int month);
}
