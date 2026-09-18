import {describe,it,expect} from "vitest";
import {safeTourImage,regionSchema} from "./types";
describe("외부 데이터 경계",()=>{
 it("공식 이미지 호스트만 허용하고 HTTPS로 정규화한다",()=>{expect(safeTourImage("http://tong.visitkorea.or.kr/a.jpg")).toBe("https://tong.visitkorea.or.kr/a.jpg");expect(safeTourImage("https://tong.visitkorea.or.kr.evil.example/a")).toBeNull();expect(safeTourImage("javascript:alert(1)")).toBeNull();});
 it("불완전한 지역 응답을 거부한다",()=>{expect(regionSchema.safeParse({code:"51130",name:"원주시"}).success).toBe(false);});
});
