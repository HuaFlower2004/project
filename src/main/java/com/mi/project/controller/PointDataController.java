package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.entity.MetaData;
import com.mi.project.entity.PointData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Controller
public class PointDataController {
    @PostMapping("/pointdata")
    @ResponseBody
    public Result<PointData> getPointData(@RequestBody PointData pointData){
        return Result.success(pointData);
    }
}
