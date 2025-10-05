package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.entity.MetaData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Controller
public class MetaDataController {
    @PostMapping("/metadata")
    @ResponseBody
    public Result<MetaData> getMetaData(@RequestBody MetaData metaData){
        return Result.success(metaData);
    }
}
