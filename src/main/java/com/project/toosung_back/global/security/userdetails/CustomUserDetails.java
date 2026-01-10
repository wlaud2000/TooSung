package com.project.toosung_back.global.security.userdetails;

import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.global.security.dto.TokenInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails extends AuthUser implements UserDetails {

    /**
     * Member 엔티티로 생성 (로그인 시)
     */
    public CustomUserDetails(Member member) {
        super(
                member.getId(),
                member.getEmail(),
                member.getLocalAuth() != null ? member.getLocalAuth().getPasswordHash() : null
        );
    }

    /**
     * AuthUser로 생성 (토큰 검증 시)
     */
    public CustomUserDetails(AuthUser authUser) {
        super(
                authUser.getMemberId(),
                authUser.getEmail(),
                authUser.getPassword()
        );
    }

    /**
     * TokenInfo로 생성 (토큰 재발급 시)
     */
    public static CustomUserDetails fromTokenInfo(TokenInfo tokenInfo) {
        AuthUser authUser = new AuthUser(
                tokenInfo.memberId(),
                tokenInfo.email(),
                null
        );
        return new CustomUserDetails(authUser);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public String getUsername() {
        return super.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
