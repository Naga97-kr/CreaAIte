package D6B.D_discover_user.user.controller;

import D6B.D_discover_user.common.dto.AuthResponse;
import D6B.D_discover_user.common.service.AuthorizeService;
import D6B.D_discover_user.user.controller.dto.*;
import D6B.D_discover_user.user.service.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthorizeService authorizeService;

    @Autowired
    public UserController(UserService userService, AuthorizeService authorizeService) {
        this.userService = userService;
        this.authorizeService = authorizeService;
    }

    /**
     * 구글로그인으로 처음 회원이 접속할 때, 회원정보를 우리 DB에 저장하기 위한 Controller
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param userReadRequestDto : (구글로그인 후) 프론트단에서 유저관련 정보를 받을 수 있다.
     */
    @PostMapping("")
    public void enrollUserInformation(@RequestHeader("Authorization") String idToken,
                                                        @RequestBody UserReadRequestDto userReadRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, userReadRequestDto.getUid());
        if(authResponse.getIsUser()) {
            userService.enrollUser(userReadRequestDto.getFcmToken(), authResponse.getDecodedToken());
        } else {
            log.info("없는 회원입니다.");
        }
    }

    /**
     * 유저 본인의 정보를 확인하는 Controller
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param uid : Firebase 통해 얻은 uid
     * @return : 유저의 세부정보를 반환
     * @throws IOException : 에러
     * @throws FirebaseAuthException : 에러
     */
    @GetMapping("/{uid}")
    public ResponseEntity<UserInfoResponseDto> readMyInfo(@RequestHeader("Authorization") String idToken,
                                                            @PathVariable String uid) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, uid);
        if(authResponse.getIsUser()) {
            return ResponseEntity.ok(UserInfoResponseDto
                    .from(userService.findUserByUid(authResponse.getDecodedToken().getUid())));
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }

    /**
     * 다른 회원의 정보를 확인
     */
    @GetMapping("/other/{target_uid}")
    public ResponseEntity<UserInfoResponseDto> readUserInfo(@PathVariable("target_uid") String targetUid) {
        try {
            return ResponseEntity.ok(UserInfoResponseDto
                    .from(userService.findUserByUid(targetUid)));
        } catch(Exception e) {
            log.info("해당 회원의 정보가 없습니다.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * 회원정보 수정(구글에서 안오는 정보 : 나이, 성별)
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param userUpdateRequestDto : 변경 혹은 추가할 정보
     * @return : 추가된 정보가 추가된 유저 정보 반환
     * @throws IOException : 에러
     * @throws FirebaseAuthException : 에러
     */
    @PutMapping("")
    public ResponseEntity<UserInfoResponseDto> updateUserInfos(@RequestHeader("Authorization") String idToken,
                                                               @RequestBody UserUpdateRequestDto userUpdateRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, userUpdateRequestDto.getUid());
        if(authResponse.getIsUser()) {
            return ResponseEntity.ok(UserInfoResponseDto
                    .from(userService.updateUserInfos(authResponse.getDecodedToken(), userUpdateRequestDto)));
        } else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * 회원 이미지 수정
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param userImgUpdateRequestDto : 유저 이미지 수정 시 RequestDto
     */
    @PutMapping("/image")
    public ResponseEntity<Object> uploadImage(@RequestHeader("Authorization") String idToken,
                              @RequestBody UserImgUpdateRequestDto userImgUpdateRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, userImgUpdateRequestDto.getUid());
        if(authResponse.getIsUser()) {
            userService.updateUserImg(userImgUpdateRequestDto);
            return ResponseEntity.ok().build();
        }
        else {
            log.info("해당 회원에 대한 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    /**
     * 회원탈퇴
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param uid : Firebase 통해 얻은 uid
     * @return : 삭제 여부를 판단해서 반환
     * @throws IOException : 에러
     * @throws FirebaseAuthException : 에러
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Object> deleteUserInfo(@RequestHeader("Authorization") String idToken,
                                                 @PathVariable String uid) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, uid);
        if(authResponse.getIsUser()) {
            userService.deleteUserInfo(authResponse.getDecodedToken());
            return ResponseEntity.ok().build();
        } else return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * 좋아요
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param loveToggleRequestDto : 좋아요
     * @throws IOException : 에러
     * @throws FirebaseAuthException : 에러
     */
    @PostMapping("/like")
    public void toggleLove(@RequestHeader("Authorization") String idToken,
                                              @RequestBody LoveToggleRequestDto loveToggleRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, loveToggleRequestDto.getUid());
        if(authResponse.getIsUser()) userService.toggleLove(loveToggleRequestDto);
    }

    /**
     * 다른 사람의 유저정보 조회
     * @param idToken : Firebase 통해서 받은 해당 유저에 대한 idToken
     * @param uid : Firebase 통해 얻은 uid
     * @param other_uid : 찾고자하는 유저의 uid
     * @return : 다른 사람의 유저 정보
     * @throws IOException : 에러
     * @throws FirebaseAuthException : 에러
     */
    @GetMapping("/{uid}/{other_uid}")
    public ResponseEntity<UserInfoResponseDto> readOtherUserInfo(@RequestHeader("Authorization") String idToken,
                                                                      @PathVariable String uid,
                                                                      @PathVariable String other_uid) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, uid);
        if(authResponse.getIsUser()) {
            return ResponseEntity.ok(UserInfoResponseDto.from(userService.findUserByUid(other_uid)));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 로그인 한 유저가 본인 또는 다른 유저의 좋아요 리스트 가져오기
     * @param idToken : 로그인 유저의 idToken
     * @param targetUid : 타겟 유저의 uid
     * @param userMadeOrLoveRequestDto : 로그인 유저의 id
     * @return : 좋아요 리스트에 오른 그림 리스트
     * @throws IOException : 예외
     * @throws FirebaseAuthException : 예외
     */
    @PostMapping("/{target_uid}/like_picture/certified")
    public ResponseEntity<List<UserPicsResponseDto>> readUserLovePicsCertified(@RequestHeader("Authorization") String idToken,
                                                                               @PathVariable("target_uid") String targetUid,
                                                                               @RequestBody UserMadeOrLoveRequestDto userMadeOrLoveRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, userMadeOrLoveRequestDto.getUid());
        if(authResponse.getIsUser()) {
            FirebaseToken decodedToken = authResponse.getDecodedToken();
            // 본인이 본인의 좋아요 누른 사진을 보는 경우
            if(Objects.equals(authResponse.getDecodedToken().getUid(), targetUid)) {
                return ResponseEntity.ok(userService.findMyLovePics(decodedToken));
            // 타인의 좋아요 누른 사진을 보는 경우
            } else {
                return ResponseEntity.ok(userService.findUserLovePicsCertified(decodedToken, targetUid));
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 비회원 유저가 다른 유저의 좋아요 리스트 가져오기
     * @param uid : Firebase 통해 얻은 uid
     * @return : 찾는 유저의 좋아요 리스트
     */
    @GetMapping("/{uid}/like_picture")
    public ResponseEntity<List<UserPicsResponseDto>> readUserLovePicsNotCert(@PathVariable String uid) {
        return ResponseEntity.ok(userService.findUserLovePics(uid));
    }

    /**
     * 로그인 한 유저가 본인 또는 다른 유저가 제작한 이미지 가져오기
     * @param idToken :
     * @param targetUid : 대상의 uid
     * @param userMadeOrLoveRequestDto :
     * @return :
     * @throws IOException :
     * @throws FirebaseAuthException :
     */
    @PostMapping("/{target_uid}/made_picture/certified")
    public ResponseEntity<List<UserPicsResponseDto>> readUserMadePicsCertified(@RequestHeader("Authorization") String idToken,
                                                                      @PathVariable("target_uid") String targetUid,
                                                                      @RequestBody UserMadeOrLoveRequestDto userMadeOrLoveRequestDto) throws IOException, FirebaseAuthException {
        AuthResponse authResponse = authorizeService.isAuthorized(idToken, userMadeOrLoveRequestDto.getUid());
        if(authResponse.getIsUser()) {
            FirebaseToken decodedToken = authResponse.getDecodedToken();
            // 본인이 본인이 제작한 이미지를 보는 경우
            if(Objects.equals(authResponse.getDecodedToken().getUid(), targetUid)) {
                return ResponseEntity.ok(userService.findMyMadePics(decodedToken));
            // 타인이 제작한 이미지를 보는 경우
            } else {
                return ResponseEntity.ok(userService.findUserMadePicsCertified(decodedToken, targetUid));
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 비회원 유저가 본인 또는 다른 유저가 제작한 이미지 가져오기
     * @param uid : Firebase 통해 얻은 uid
     * @return : 유저가 만든 이미지 정보
     */
    @GetMapping("/{uid}/made_picture")
    public ResponseEntity<List<UserPicsResponseDto>> readUserMadePicsNotCert(@PathVariable String uid){
        return ResponseEntity.ok(userService.findUserMadePics(uid));
    }

    //***************************************여기서부턴 MSA 통신***********************************************//

    /**
     * 삭제한 그림의 좋아요 기록을 disable 하기 위함
     */
    @PostMapping("/like/delete/{picture_id}")
    public ResponseEntity<Object> deleteLoveByPictureDead(@PathVariable("picture_id") Long pictureId) {
        userService.deActiveLove(pictureId);
        return ResponseEntity.ok().build();
    }

    /**
     * 로그인 유저가 그림 리스트를 조회할 때, 그림의 생성자와 좋아요 여부를 picture 서버에 알려주는 API
     * @param loveCheckAndMakerRequestDtos : 현재로그인한 유저의 uid, 레이아웃될 그림의 id 목록, 그림 생성자 uid
     * @return : 이미지의 생성자 이름 목록, 해당 이미지에 대한 유저의 좋아요 여부
     */
    @PostMapping("/find_love_check_maker_name")
    public List<LoveCheckAndMakerResponseDto> findLoveChecksAndMakers(@RequestBody List<LoveCheckAndMakerRequestDto> loveCheckAndMakerRequestDtos) {
        return userService.findLoveChecksAndMakers(loveCheckAndMakerRequestDtos);
    }

    /**
     * 비회원 유저가 그림 리스트를 조회할 때, 그림의 생성자를 picture 서버에 알려주는 API
     * @param makerUids : 이미지의 생성자 uid 목록
     * @return : 이미지의 생성자 이름 목록을 반환한다.
     */
    @PostMapping("/find_maker_name")
    public List<String> findMakers(@RequestBody List<String> makerUids) {
        return userService.findMakers(makerUids);
    }

    /**
     * 알람 서버에서 uid 통해서 fcmToken 값을 얻을 때 보내는 API
     * @param uid : 토큰값을 알고자하는 유저의 uid
     * @return : fcmToken(String)
     */
    @GetMapping("/fcm/{uid}")
    public String getFCMTokenByUserUid(@PathVariable String uid) {
        return userService.getFCMTokenByUserUid(uid);
    }

    @GetMapping("/test")
    public String test() {
        return "test2";
    }

}
