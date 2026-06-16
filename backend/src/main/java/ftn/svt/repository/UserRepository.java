package ftn.svt.repository;

import ftn.svt.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    @Query("""
                select distinct u
                from users u
                where concat(u.firstName,' ',u.lastName) like %?1%
                    or u.username like %?1%
                    or u.phoneNumber like %?1%
            """)
    Page<User> findAllFiltered(
            @Param("searchStr") String search,
            Pageable pageable);
}
