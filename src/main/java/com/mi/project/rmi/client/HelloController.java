package com.mi.project.rmi.client;

import com.mi.project.rmi.api.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * @author 31591
 */
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello(String name) throws RemoteException, MalformedURLException, NotBoundException {
        HelloService svc = (HelloService) Naming.lookup("rmi://192.168.181.152:1099/HelloService");
        return svc.sayHello(name == null ? "world" : name);
    }
}
