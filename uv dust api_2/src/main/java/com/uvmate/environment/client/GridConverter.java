package com.uvmate.environment.client;

/**
 * 위경도(lat/lon) → 기상청 격자좌표(nx, ny) 변환 유틸.
 * 기상청 단기예보/생활기상지수 API는 위경도가 아니라 격자좌표(또는 지역코드)를 요구하기 때문에
 * 반드시 이 변환을 거쳐야 함.
 * 공식 출처: 기상청 단기예보 API 활용가이드에 포함된 LCC DFS 좌표변환 알고리즘 (표준 공식, 값 불변)
 */
public class GridConverter {

    private static final double RE = 6371.00877; // 지구 반경(km)
    private static final double GRID = 5.0;       // 격자 간격(km)
    private static final double SLAT1 = 30.0;     // 투영 위도1
    private static final double SLAT2 = 60.0;     // 투영 위도2
    private static final double OLON = 126.0;     // 기준점 경도
    private static final double OLAT = 38.0;      // 기준점 위도
    private static final double XO = 43;          // 기준점 X좌표
    private static final double YO = 136;         // 기준점 Y좌표

    public static class Grid {
        public final int nx;
        public final int ny;

        public Grid(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }
    }

    public static Grid toGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new Grid(nx, ny);
    }
}
