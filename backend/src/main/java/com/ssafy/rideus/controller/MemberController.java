package com.ssafy.rideus.controller;

import com.ssafy.rideus.config.security.auth.CustomUserDetails;
import com.ssafy.rideus.dto.member.request.MemberMoreInfoReq;
import com.ssafy.rideus.dto.member.response.MemberMeRes;
import com.ssafy.rideus.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberMeRes> loginMemberInformation(@ApiIgnore @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(memberService.findByLoginMember(user.getId()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMemberMyself(@ApiIgnore @AuthenticationPrincipal CustomUserDetails user) {
        memberService.deleteMember(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{nickname}")
    public ResponseEntity<Boolean> checkNicknameDuplicate(@PathVariable String nickname) {
        return ResponseEntity.ok(memberService.checkNicknameDuplicate(nickname));
    }

    @PutMapping("/info")
    public ResponseEntity<Void> moreInformation(@Valid @RequestBody MemberMoreInfoReq memberMoreInfoReq, @ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        memberService.updateMoreInformation(memberMoreInfoReq, member.getId());
        return ResponseEntity.ok().build();
    }

}