package com.ganzithon.backend.counseling.adapter.out.persistence;

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

    @Modifying
    @Query("delete from CounselingMessageEntity m where m.sessionId = :sessionId")
    int deleteBySession(@Param("sessionId") long sessionId);
}

interface CounselingResultJpaRepository extends JpaRepository<CounselingResultEntity, Long> {
    @Modifying
    @Query("delete from CounselingResultEntity r where r.sessionId = :sessionId")
    int deleteBySession(@Param("sessionId") long sessionId);
}
