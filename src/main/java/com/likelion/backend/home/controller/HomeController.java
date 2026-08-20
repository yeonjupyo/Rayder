package com.likelion.backend.home.controller;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.environment.client.RegionResolver;
import com.likelion.backend.home.dto.HomeResponse;
import com.likelion.backend.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final RegionResolver regionResolver;

    /**
     * 기상청 지점코드(areaNo)를 직접 받거나, 지역명(sido/gugun)으로 받아 서버에서 지점코드로 변환한다.
     * 클라이언트가 kma-area-codes.csv 를 들고 있을 필요가 없도록 후자를 권장한다.
     */
    @GetMapping
    public ResponseEntity<HomeResponse> getHome(
            @RequestParam int userId,
            @RequestParam(required = false) String areaNo,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String gugun) {
        return ResponseEntity.ok(homeService.getHome(userId, resolveAreaNo(areaNo, sido, gugun)));
    }

    private String resolveAreaNo(String areaNo, String sido, String gugun) {
        if (StringUtils.hasText(areaNo)) {
            return areaNo;
        }
        if (StringUtils.hasText(sido) && StringUtils.hasText(gugun)) {
            return regionResolver.resolve(sido, gugun).areaNo();
        }
        throw new BusinessException("INVALID_REQUEST_PARAMETER",
                "areaNo, or both sido and gugun, is required", HttpStatus.BAD_REQUEST);
    }
}
