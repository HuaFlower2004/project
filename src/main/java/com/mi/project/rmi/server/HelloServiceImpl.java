package com.mi.project.rmi.server;

import com.mi.project.rmi.api.HelloService;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;

/**
 * @author 31591
 */
@Component
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl() throws RemoteException {
        // 不要再调用 super()，不继承 UnicastRemoteObject 就不会自动 export
    }

    @Override
    public String sayHello(String name) {
        return "远程测试成功";
    }
}
