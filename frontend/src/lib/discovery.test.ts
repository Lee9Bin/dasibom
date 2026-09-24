import {describe,it,expect} from "vitest";
import {readFileSync} from "node:fs";
import {photoMonth} from "./photo-utils";
describe("discovery data quality",()=>{
 it("does not display month zero",()=>{expect(photoMonth("202100")).toBe("2021년 촬영 · 월 미제공");expect(photoMonth("202413")).toBe("2024년 촬영 · 월 미제공");expect(photoMonth("202408")).toBe("2024.08 촬영");});
 it("provides a boundary for every service region, including Gury e and Incheon",()=>{
  const codes=new Set(["V1__initial_schema.sql","V2__live_api_and_nationwide_regions.sql"].flatMap(file=>Array.from(readFileSync(`../backend/src/main/resources/db/migration/${file}`,"utf8").matchAll(/\('(\d{5})'/g),m=>m[1])));
  const geo=JSON.parse(readFileSync("public/data/korea-sgg.json","utf8"));
  const boundaries=new Set(geo.features.map((f:{properties:{sgg:string}})=>f.properties.sgg));
  expect(codes.size).toBe(252);expect(boundaries).toEqual(codes);
 });
});
