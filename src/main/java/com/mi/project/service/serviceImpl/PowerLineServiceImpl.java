package com.mi.project.service.serviceImpl;

import com.mi.project.entity.PowerLine;
import com.mi.project.mapper.PowerLineMapper;
import com.mi.project.service.IPowerLineService;
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
public class PowerLineServiceImpl extends ServiceImpl<PowerLineMapper, PowerLine> implements IPowerLineService {

}
