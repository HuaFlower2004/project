package com.mi.project.rmi.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author 31591
 */
public interface HelloService extends Remote {
    String sayHello(String name) throws RemoteException;
}
