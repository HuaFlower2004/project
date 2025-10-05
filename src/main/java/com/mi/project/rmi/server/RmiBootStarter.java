package com.mi.project.rmi.server;

import com.mi.project.rmi.api.HelloService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Configuration
public class RmiBootStarter {
    private final HelloService helloService; // 这是实现类的 Spring Bean

    public RmiBootStarter(HelloService helloService) {
        this.helloService = helloService;
    }

    @PostConstruct
    public void start() throws Exception {
        // 告诉 RMI “回连地址”，改成你的内网 IP
        System.setProperty("java.rmi.server.hostname", "192.168.181.152");

        // 固定导出端口，避免随机端口被防火墙挡住
        int objectPort = 20001;
        Remote stub = UnicastRemoteObject.exportObject((Remote) helloService, objectPort);

        // 启动或获取注册中心（已启动则不会报错）
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(1099);
        }

        // 用“简名”绑定，不要写 rmi://.../HelloService
        registry.rebind("HelloService", stub);
        System.out.println("✅ RMI service bound as 'HelloService' on 1099, objPort=" + objectPort);
    }
}
