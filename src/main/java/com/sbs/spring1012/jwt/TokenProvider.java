package com.sbs.spring1012.jwt;

import com.sbs.spring1012.dto.AccessTokenDto;
import com.sbs.spring1012.dto.TokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider {
    private static final String AUTHORITIES_KEY="auth"; //토큰에 저장되는 권한 정보의 key
    private static final String BEARER_TYPE = "Bearer"; // 토큰의 타입
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7L; // 7일
    private final Key key; // 토큰 서명을 하기 위한 Key

    //주의점 : @Value 어노테이션은 springframework의 어노테이션이다.
    public TokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512); // HS512 알고리즘을 사용하는 키 생성
    }

    // 토큰 생성
    public TokenDto generateTokenDto(Authentication authentication){
        //권한 정보 문자열 생성,
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime(); // 현재 시간
        // 토큰 만료 시간 설정
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME); // 액세스 토큰 만료 시간
        Date refreshTokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME); // 리프레시 토큰 만료 시간


        log.info("TP getName {}",authentication.getName());
        log.info("TP getPrincipal {}",authentication.getDetails());
        log.info("TP getAuthorities {}",authentication.getAuthorities());


        // 토큰 생성
        String accessToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        // 리프레시 토큰 생성
        String refreshToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        log.info("TOKENPRO RFTK : {}", refreshToken);

        // 토큰 정보를 담은 TokenDto 객체 생성
        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExpiresIn.getTime())
                .build();
    };

    public AccessTokenDto generateAccessTokenDto(Authentication authentication) {
        //권한 정보 문자열 생성,
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime(); // 현재 시간
        // 토큰 만료 시간 설정
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME); // 액세스 토큰 만료 시간

        // 토큰 생성
        String accessToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return AccessTokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .build();
    }


    public Authentication getAuthentication(String accessToken){
        //토큰 복호화
        Claims claims = parseClaims(accessToken);

        // 복호화에 실패하면
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }



        // 토큰에 담긴 권한 정보들을 가져옴
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 권한 정보들을 이용해 유저 객체를 만들어서 반환, 여기서 User 객체는 UserDetails 인터페이스를 구현한 객체
        User principal = new User(claims.getSubject(),"", authorities);

        // 유저 객체, 토큰, 권한 정보들을 이용해 인증 객체를 생성해서 반환
        return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
    }
    // 토큰의 유효성 검증
    public boolean validateToken(String token){
        try {
            io.jsonwebtoken.Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        }catch (SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        }catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        }catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        }catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    //토큰 복호화
    private Claims parseClaims(String accessToken) {
        try {
            return io.jsonwebtoken.Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        }catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}