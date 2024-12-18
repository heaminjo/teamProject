package com.sbs.spring1012.repository;

import com.sbs.spring1012.entity.Member;
import com.sbs.spring1012.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    boolean existsByRefreshToken(String refreshToken);
    boolean existsByMember(Member member);
    Optional<RefreshToken> findByMember(Member member);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.member = :member")
    void deleteByMember(@Param("member") Member member);
}
