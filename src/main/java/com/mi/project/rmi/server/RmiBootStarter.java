package com.mi.project.rmi.server;

import com.mi.project.rmi.api.HelloService;
import com.mi.project.rmi.api.PowerLineAnalysisService;
import com.mi.project.rmi.api.FileRmiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI服务启动器
 * 注册多个远程服务
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RmiBootStarter {
    
    private final HelloService helloService;
    private final PowerLineAnalysisService powerLineAnalysisService;
    private final FileRmiService fileRmiService;

    @PostConstruct
    public void start() throws Exception {
        // 告诉 RMI "回连地址"，改成你的内网 IP
        System.setProperty("java.rmi.server.hostname", "192.168.181.152");

        // 启动或获取注册中心（已启动则不会报错）
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(1099);
            log.info("RMI注册中心启动成功，端口: 1099");
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(1099);
            log.info("连接到现有RMI注册中心，端口: 1099");
        }

        // 注册HelloService
        int helloPort = 20001;
        Remote helloStub = UnicastRemoteObject.exportObject((Remote) helloService, helloPort);
        registry.rebind("HelloService", helloStub);
        log.info("HelloService 已注册，端口: {}", helloPort);

        // 注册PowerLineAnalysisService
        int analysisPort = 20002;
        Remote analysisStub = UnicastRemoteObject.exportObject((Remote) powerLineAnalysisService, analysisPort);
        registry.rebind("PowerLineAnalysisService", analysisStub);
        log.info("PowerLineAnalysisService 已注册，端口: {}", analysisPort);

        // 注册FileRmiService
        int filePort = 20003;
        Remote fileStub = UnicastRemoteObject.exportObject((Remote) fileRmiService, filePort);
        registry.rebind("FileRmiService", fileStub);
        log.info("FileRmiService 已注册，端口: {}", filePort);

        log.info("所有RMI服务启动完成！");
        log.info("服务列表:");
        log.info("  - HelloService: rmi://192.168.181.152:1099/HelloService");
        log.info("  - PowerLineAnalysisService: rmi://192.168.181.152:1099/PowerLineAnalysisService");
        log.info("  - FileRmiService: rmi://192.168.181.152:1099/FileRmiService");
    }
}
