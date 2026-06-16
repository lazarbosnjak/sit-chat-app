package ftn.svt.repository;

import ftn.svt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    @Query("""
                select distinct u
                from User u
                where u.id <> :principalUserId
                    and u.enabled
                    and (
                        upper(concat(u.firstName,' ',u.lastName)) like concat('%', upper(?1), '%')
                        or upper(u.username) like concat('%', upper(?1), '%')
                        or u.phoneNumber like concat('%', upper(?1), '%')
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
            """)
    Collection<User> findAllFiltered(
            @Param("searchStr") String search,
            @Param("principalUserId") UUID principalUserId);
}
