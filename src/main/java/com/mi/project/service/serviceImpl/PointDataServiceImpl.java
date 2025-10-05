package com.mi.project.service.serviceImpl;

import com.mi.project.entity.PointData;
import com.mi.project.mapper.PointDataMapper;
import com.mi.project.service.IPointDataService;
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
public class PointDataServiceImpl extends ServiceImpl<PointDataMapper, PointData> implements IPointDataService {

}
