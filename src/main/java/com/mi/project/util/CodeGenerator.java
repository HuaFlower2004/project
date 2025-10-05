package com.mi.project.util;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/project", "root", "mijiajie20040820")
                .globalConfig(builder -> {
                    builder.author("JackBlack") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir(System.getProperty("user.dir") + "/src/main/java"); // 指定输出目录
                })
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.SMALLINT) {
                                // 自定义类型转换
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .packageConfig(builder ->
                        builder.parent("com.mi.project") // 设置父包名
                                .entity("entity")
                                .service("service")
                                .mapper("mapper")
                                .serviceImpl("service.serviceImpl")
                                .controller("controller")
                                .pathInfo(Collections.singletonMap(OutputFile.xml, "src/main/resources/mapper")) // 设置mapperXml生成路径
                )
                .strategyConfig(builder ->
                        builder.addInclude("power_tower") // 设置需要生成的表名
                                .addTablePrefix() // 设置过滤表前缀
                                // 实体类策略配置
                                .entityBuilder()
                                .enableFileOverride() // 开启实体类文件覆盖
                                // Controller策略配置
                                .controllerBuilder()
                                .enableFileOverride() // 开启Controller文件覆盖
                                // Service策略配置
                                .serviceBuilder()
                                .enableFileOverride() // 开启Service文件覆盖
                                // Mapper策略配置
                                .mapperBuilder()
                                .enableFileOverride() // 开启Mapper文件覆盖
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
