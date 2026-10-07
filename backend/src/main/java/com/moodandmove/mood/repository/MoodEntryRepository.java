package com.moodandmove.mood.repository;

import com.moodandmove.mood.domain.entity.MoodEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodEntryRepository extends JpaRepository<MoodEntry,Long> {

    List<MoodEntry> findAllByUser_IdAndEntryDateGreaterThanEqualAndEntryDateLessThanAndDeletedAtIsNullOrderByEntryDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    @EntityGraph(attributePaths = "emotion")
    Optional<MoodEntry> findByIdAndUser_IdAndDeletedAtIsNull(
            Long moodEntryId,
            Long userId
    );

    boolean existsByUser_IdAndEntryDate(
            Long userId,
            LocalDate entryDate
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m
        from MoodEntry m
        where m.id = :moodEntryId
          and m.user.id = :userId
          and m.deletedAt is null
        """)
    Optional<MoodEntry> findOwnedForUpdate(
            @Param("moodEntryId") Long moodEntryId,
            @Param("userId") Long userId
    );
}
