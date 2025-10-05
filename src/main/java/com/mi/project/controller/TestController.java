package com.mi.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.project.common.Result;
import com.mi.project.dto.fileDTO.TestDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TestController {
    @PostMapping("/modeldata")
    public Result<TestDTO> getModelData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObj1 = mapper.readValue(
                java.nio.file.Files.newInputStream(
                        java.nio.file.Paths.get("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\point_json\\all_power_lines_20250722_164156.json")),
                Object.class);
        Object jsonObj2 = mapper.readValue(
                java.nio.file.Files.newInputStream(
                        java.nio.file.Paths.get("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\ransac_json\\powerline_curves-1.json")),
                Object.class);
        TestDTO testDTO = new TestDTO();
        testDTO.setJson1(jsonObj1);
        testDTO.setJson2(jsonObj2);
        return Result.success(testDTO);
    }
    @GetMapping("/who")
    public String who() { return "I am 192.168.181.152"; }

}