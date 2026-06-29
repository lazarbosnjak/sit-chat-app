package ftn.svt.repository;

import ftn.svt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Instant rangeStart,
            Instant rangeEnd
    );

    @Query(
            value = """
                    select date_trunc(:bucket, u.created_at at time zone 'UTC'), count(*)
                    from users u
                    where u.created_at >= :rangeStart and u.created_at < :rangeEnd
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countRegistrationsByBucket(
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("bucket") String bucket
    );

    @Query("""
                select u
                from User u
                where u.id <> :principalUserId
                    and u.enabled
                    and (
                        :filterBySearch = false
                        or upper(concat(u.firstName,' ',u.lastName)) like concat('%', upper(:searchStr), '%')
                        or upper(u.firstName) like concat('%', upper(:searchStr), '%')
                        or upper(u.lastName) like concat('%', upper(:searchStr), '%')
                        or upper(u.username) like concat('%', upper(:searchStr), '%')
                        or u.phoneNumber like concat('%', :searchStr, '%')
                    )
                    and (
                        :filterByProfilePicture = false
                        or (:hasProfilePicture = true
                            and u.pfpUrl is not null
                            and u.pfpUrl <> ''
                            and u.pfpUrl <> :defaultProfilePictureUrl)
                        or (:hasProfilePicture = false
                            and (u.pfpUrl is null
                                or u.pfpUrl = ''
                                or u.pfpUrl = :defaultProfilePictureUrl))
                    )
                    and (
                        :filterByLastActive = false
                        or (u.lastActiveAt >= :lastActiveFrom and u.lastActiveAt < :lastActiveBefore)
                    )
                    and not exists (
                        select 1
                        from Chat chat
                        join chat.members m1
                        join chat.members m2
                        where chat.type = 'DIRECT'
                            and m1.user.id = :principalUserId
                            and m2.user.id = u.id
                    )
                order by
                    case when u.lastActiveAt is null then 1 else 0 end,
                    u.lastActiveAt desc,
                    u.firstName,
                    u.lastName,
                    u.username
            """)
    Collection<User> findAllFiltered(
            @Param("filterBySearch") boolean filterBySearch,
            @Param("searchStr") String search,
            @Param("filterByProfilePicture") boolean filterByProfilePicture,
            @Param("hasProfilePicture") boolean hasProfilePicture,
            @Param("filterByLastActive") boolean filterByLastActive,
            @Param("lastActiveFrom") java.time.Instant lastActiveFrom,
            @Param("lastActiveBefore") java.time.Instant lastActiveBefore,
            @Param("defaultProfilePictureUrl") String defaultProfilePictureUrl,
            @Param("principalUserId") UUID principalUserId);
}
