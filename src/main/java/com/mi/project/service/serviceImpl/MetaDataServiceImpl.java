package com.mi.project.service.serviceImpl;

import com.mi.project.entity.MetaData;
import com.mi.project.mapper.MetaDataMapper;
import com.mi.project.service.IMetaDataService;
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
public class MetaDataServiceImpl extends ServiceImpl<MetaDataMapper, MetaData> implements IMetaDataService {

}
