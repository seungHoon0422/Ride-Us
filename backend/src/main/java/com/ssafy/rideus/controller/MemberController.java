package com.ssafy.rideus.controller;

import com.ssafy.rideus.config.security.auth.CustomUserDetails;
import com.ssafy.rideus.domain.Record;
import com.ssafy.rideus.dto.member.request.MemberMoreInfoReq;
import com.ssafy.rideus.dto.member.request.MemberUpdateRequest;
import com.ssafy.rideus.dto.member.response.MemberMeRes;
import com.ssafy.rideus.dto.record.response.MyRideRecordRes;
import com.ssafy.rideus.dto.record.response.RecordTotalResponse;
import com.ssafy.rideus.dto.tag.response.MemberTagResponse;
import com.ssafy.rideus.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberMeRes> loginMemberInformation(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(memberService.findByLoginMember(member.getId()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMemberMyself(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        memberService.deleteMember(member.getId());
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

    @PatchMapping("/modify")
    public ResponseEntity<Void> modifyMemberInformation(@Valid @RequestBody MemberUpdateRequest request, @ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        memberService.updateMember(request, member.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<MyRideRecordRes>> getRecentRecord(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(memberService.getRecentRecord(member.getId()));
    }

    @GetMapping("/total-record")
    public ResponseEntity<RecordTotalResponse> getTotalRecord(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(memberService.getTotalRecord(member.getId()));
    }

    @GetMapping("/my-tag")
    public ResponseEntity<List<MemberTagResponse>> getMyTag(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(memberService.getMyTag(member.getId()));
    }

    @GetMapping("/recent/my-ride")
    public ResponseEntity<List<MyRideRecordRes>> recentMyRide(@ApiIgnore @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(memberService.getMyRideRecord(member.getId()));
    }
}
