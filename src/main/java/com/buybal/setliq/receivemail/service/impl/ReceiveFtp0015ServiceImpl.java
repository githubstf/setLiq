package com.buybal.setliq.receivemail.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buybal.epay.model.Tbank;
import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.model.Tdatadic;
import com.buybal.epay.service.TbankService;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.epay.service.TseqService;
import com.buybal.epay.service.common.DataDicService;
import com.buybal.setliq.receivemail.service.ReceiveFtpService;
import com.buybal.util.DateUtil;
import com.buybal.util.FtpFileDealService;
import com.buybal.util.PropertiseUtil;
import com.buybal.util.StringUtil;

//import org.springframework.transaction.annotation.Transactional;

public class ReceiveFtp0015ServiceImpl implements ReceiveFtpService {

	private static Logger logger = LoggerFactory.getLogger(ReceiveFtp0015ServiceImpl.class);

	private TbankchefileService service = new TbankchefileService();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private int count = 0;
	StringBuffer bodytext = new StringBuffer();// 存放邮件内容
	@Override
	public int receiveAllFtp(String ip,Integer port,String name,String passWord,String bankId) {
		// 获取全部接收邮箱
		if (count != 0) {
			return 0;
		}
		try {
			count = 1;
			receiveFtp( ip, port, name, passWord, bankId);

		} catch (Exception e) {
			logger.error("exception", e);
		} finally {
			count = 0;
		}

		return 1;
	}

	/**
	 * 接收邮件
	 * 
	 * @param 邮箱的用户名和密码
	 * @return 无
	 */
	private void receiveFtp(String ip,Integer port,String name,String passWord,String bankId) {
		 String org = PropertiseUtil.getString("setLiq", "ORG_0015");
        DataDicService dds = new DataDicService();
        Tdatadic Tdatadic = dds.getDataDic("FILE_URL", "sharefile");
		if (Tdatadic == null) {
			logger.error("字典表文件存放路径参数不存在FILE_URL+sharefile");
			return;
		}
		String filePath = Tdatadic.getDIC_NAME() + "bankRecon/" + bankId + "/";
		String filename =org+"_"+DateUtil.fomatDate(new Date(), "yyyyMMdd")+"_01.ZIP";
		FtpFileDealService FtpFileDealService = new FtpFileDealService(ip, port, name,passWord);
		boolean ftpdown = FtpFileDealService.ftpdownload(ip, port, "/home/epaysch", filePath, filename, "PASV");
		if(!ftpdown){
			logger.error("文件下载失败"+filename);
			return;
		}
		List<String> nameList = new ArrayList<String>();
		nameList.add(filename);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("nameList", nameList);
		map.put("bankId", bankId);
		try {
			saveFtp(map);
		} catch (UnsupportedEncodingException e) {
			logger.error("ftp文件下载入库异常", e);
		}
		

	}
	
	/**
	 * 入库
	 * 
	 * @param fileName
	 *            ：附件名称
	 * @param filePath
	 *            ：邮件附件存放基路径
	 * @throws UnsupportedEncodingException
	 * 
	 */
	private int saveFtp(Map<String, Object> map) throws UnsupportedEncodingException {
		List<String> files = (List<String>) map.get("nameList");
		if (files == null || files.size() < 1) {
			return 0;
		}
		for (int i = 0; i < files.size(); i++) {
			Tbankchkfile tbankchkfile = new Tbankchkfile();
			String fileName = files.get(i);
			String bankId = (String) map.get("bankId");
			fileName = MimeUtility.decodeText(fileName);
			tbankchkfile.setBANKID(bankId);
			tbankchkfile.setFILENAME(fileName);
			int a = service.selectTbankchkfile(tbankchkfile);
			if (a > 0) {
				logger.info("已经入库fileName="+fileName+"，不能重复入库");
				continue;
			}
			Date date = new Date();
			TseqService ts = new TseqService();
			tbankchkfile.setBATCHID(ts.getAccBatchId());
			DataDicService dds = new DataDicService();
			Tdatadic Tdatadic = dds.getDataDic("FILE_URL", "sharefile");
			if (Tdatadic == null) {
				logger.error("字典表文件存放路径参数不存在FILE_URL+sharefile");
			}
			String filePath = Tdatadic.getDIC_NAME() + "bankRecon/" + bankId + "/";
			tbankchkfile.setFILEPATH(filePath);
			TbankService bankService = new TbankService();
			Tbank bank = bankService.selectByPrimaryKey(bankId);
			String bankName = bank.getNAME();
			tbankchkfile.setPLATTIME(date);
			tbankchkfile.setBANKNAME(bankName);
			tbankchkfile.setSTATE(0);
			service.deleteByKey(bankId, fileName);// 先删除记录
			service.insertSelective(tbankchkfile);
			logger.info("下载附件成功");
		}
		return 1;
	}


}
