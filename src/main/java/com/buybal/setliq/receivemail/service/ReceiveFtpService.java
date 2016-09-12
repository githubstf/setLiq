package com.buybal.setliq.receivemail.service;


/*
 * 接收邮箱信息
 */
public interface ReceiveFtpService {
	
	int receiveAllFtp(String ip,Integer port,String name,String passWord,String bankId);

}
