package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.entity.PowerLine;
import com.mi.project.entity.PowerTower;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class PowerTowerController {
    @PostMapping("/powertower")
    @ResponseBody
    public Result<PowerTower> getPowerTower(@RequestBody PowerTower powerTower){
        return Result.success(powerTower);
    }
}
