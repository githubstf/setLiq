package com.buybal.setliq.receivemail.service;


/*
 * 接收邮箱信息
 */
public interface ReceiveMailService {
	/**
	 * 接收邮箱信息
	 * @return
	 */
	int receiveAllMail(String userName,String passWord,String mailHost,Integer port,String bankId);
	/**
	 * 验证邮箱正误
	 * @param userName
	 * @param passWord
	 * @param mailHost
	 * @param port
	 * @return
	 */
	public boolean validateMail(String userName, String passWord, String mailHost ,Integer port);
}
