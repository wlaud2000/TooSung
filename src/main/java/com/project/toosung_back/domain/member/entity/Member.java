package com.project.toosung_back.domain.member.entity;

import com.project.toosung_back.domain.auth.entity.LocalAuth;
import com.project.toosung_back.domain.auth.entity.SocialAuth;
import com.project.toosung_back.global.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "member")
public class Member extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private LocalAuth localAuth;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private SocialAuth socialAuth;

    public boolean isSocialLogin() {
        return socialAuth != null;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
