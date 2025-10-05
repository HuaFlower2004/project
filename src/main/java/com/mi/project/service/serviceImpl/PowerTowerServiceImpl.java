package com.mi.project.service.serviceImpl;

import com.mi.project.entity.PowerTower;
import com.mi.project.mapper.PowerTowerMapper;
import com.mi.project.service.IPowerTowerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
public class PowerTowerServiceImpl extends ServiceImpl<PowerTowerMapper, PowerTower> implements IPowerTowerService {

}
