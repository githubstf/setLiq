package com.buybal.setliq.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.setliq.receivemail.service.ReceiveFtpService;
import com.buybal.setliq.receivemail.service.ReceiveMailService;
import com.buybal.setliq.receivemail.service.ReconciliationCheckService;
import com.buybal.setliq.receivemail.service.impl.ReceiveFtp0015ServiceImpl;
import com.buybal.setliq.receivemail.service.impl.ReconciliationCheck0015ServiceImpl;
import com.buybal.util.DateUtil;
import com.buybal.util.PropertiseUtil;

public class Reconciliation0015Service {
	Logger logger = LoggerFactory.getLogger(Reconciliation0015Service.class);
	private String bank = PropertiseUtil.getString("setLiq", "0015");
	private TbankchefileService bcf = new TbankchefileService();
	private ReconciliationCheckService rCkService = new ReconciliationCheck0015ServiceImpl();
	//邮件下载
	public void receiveFtp(){
		ReceiveFtpService service = new ReceiveFtp0015ServiceImpl();
		String[] str = bank.split("\\|");
		String ip = str[0];
		int port = Integer.parseInt(str[1]);
		String name = str[2];
		String pwd = str[3];
		String bankId = str[4];
		service.receiveAllFtp(ip, port, name, pwd, bankId);
	}
   //加载
	public void loadFile(String bankId) {
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("bankId", bankId);
	    map.put("state", 0);
		List<Tbankchkfile> files = bcf.getTbankchkfileListByBankId(map);
		if (files.size() < 1) {
			logger.info("没有需要加载的对账文件！bankId="+bankId);
             return;
		}
		for (Tbankchkfile file : files) {
		String tbatchId = file.getBATCHID();
		//加载对账文件
		Map<String,Object> resMap = rCkService.loadCheckFile(tbatchId, bankId);
		if(!"0000".equals(resMap.get("errorCode"))){
			logger.info("加载失败bankId="+bankId+",tbatchId="+tbatchId+",error="+resMap.get("errorMsg"));
			continue;
		}
		//对账处理
		reconciliationCheck(file);
		}
	}
    //对账
	private int reconciliationCheck(Tbankchkfile file) {
		try {
		String bankIds = file.getBANKID();
		String tbatchId = file.getBATCHID();
		String fileName= file.getFILENAME();
		String liqDate= fileName.substring(9,9+8);
		 Map<String,Object> resMap = rCkService.reconciliationCheck(bankIds, tbatchId,null,liqDate);
			 if(!"0000".equals(resMap.get("errorCode"))){
				logger.info("对账失败bankId="+bankIds+",tbatchId="+tbatchId+",error="+resMap.get("errorMsg"));
				 return 0;
			 }
		} catch (Exception e) {
			logger.error("对账异常！",e);
			return 0;
		}
		return 1;
	}

}
