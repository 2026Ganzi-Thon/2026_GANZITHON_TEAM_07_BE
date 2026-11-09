package com.ganzithon.backend.malangi.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserMalangiJpaRepository extends JpaRepository<UserMalangiEntity, Long> {
    List<UserMalangiEntity> findAllByUserIdOrderByMalangiType(long userId);

    java.util.Optional<UserMalangiEntity> findByUserIdAndMalangiType(long userId, String malangiType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select malangi from UserMalangiEntity malangi where malangi.userId = :userId "
            + "and malangi.malangiType = :malangiType")
    java.util.Optional<UserMalangiEntity> findByUserIdAndMalangiTypeForUpdate(
            @Param("userId") long userId, @Param("malangiType") String malangiType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select malangi from UserMalangiEntity malangi where malangi.userId = :userId")
    List<UserMalangiEntity> findAllByUserIdForUpdate(@Param("userId") long userId);
}
