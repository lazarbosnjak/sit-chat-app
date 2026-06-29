package ftn.svt.repository;

import ftn.svt.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {
    @Query("""
            select count(distinct activity.user.id)
            from UserActivity activity
            where activity.occurredAt >= :rangeStart and activity.occurredAt < :rangeEnd
            """)
    long countActiveUsers(
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd
    );

    @Query(
            value = """
                    select date_trunc(:bucket, activity.occurred_at at time zone 'UTC'),
                           count(distinct activity.user_id)
                    from user_activities activity
                    where activity.occurred_at >= :rangeStart and activity.occurred_at < :rangeEnd
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countActiveUsersByBucket(
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("bucket") String bucket
    );
}
