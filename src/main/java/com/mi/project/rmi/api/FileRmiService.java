package com.mi.project.rmi.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author 31591
 */
public interface FileRmiService extends Remote {
    /**
     * 远程上传文件
     * @param fileData 文件二进制
     * @param filename 文件名
     * @param userName 用户名
     * @return 远程存储路径
     */
    String uploadFile(byte[] fileData, String filename, String userName) throws RemoteException;
}
