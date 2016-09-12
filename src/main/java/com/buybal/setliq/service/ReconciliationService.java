package com.buybal.setliq.service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.setliq.receivemail.service.ReceiveMailService;
import com.buybal.setliq.receivemail.service.ReconciliationCheckService;
import com.buybal.setliq.receivemail.service.impl.ChinaTvPayReconciliationCheckServiceImpl;
import com.buybal.setliq.receivemail.service.impl.ReceiveMailServiceImpl;
import com.buybal.util.PropertiseUtil;

public class ReconciliationService {
	Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
	private String bank = PropertiseUtil.getString("setLiq", "0003");
	private TbankchefileService bcf = new TbankchefileService();
	private ReconciliationCheckService rCkService = new ChinaTvPayReconciliationCheckServiceImpl();
	//下载邮件
	public void receiveMail(){
		ReceiveMailService service = new ReceiveMailServiceImpl();
		String[] str = bank.split("\\|");
		String name = str[0];
		String pwd = str[1];
		String pop = str[2];
		int port = Integer.parseInt(str[3]);
		String bankId = str[4];
		service.receiveAllMail(name, pwd, pop, port, bankId);
	}
   //加载
	public void loadFile(String bankId) {
	//	List<Tbankchkfile> files = bcf.getTbankchkfileList();
		Map<String,Object> map = new HashMap();
	    map.put("bankId", bankId);
	    map.put("state", 0);
		List<Tbankchkfile> files = bcf.getTbankchkfileListByBankId(map);
		if (files.size() < 1) {
			logger.info("没有需要加载的对账文件！bankId="+bankId);
             return;
		}
		for (Tbankchkfile file : files) {
		String tbatchId = file.getBATCHID();
		 
		 Map<String,Object> resMap = rCkService.loadCheckFile(tbatchId, bankId);
		if(!"0000".equals(resMap.get("errorCode"))){
			logger.info("加载失败bankId="+bankId+",tbatchId="+tbatchId+",error="+resMap.get("errorMsg"));
			continue;
		}
		reconciliationCheck(file);
		}
	}
    //对账
	private int reconciliationCheck(Tbankchkfile file) {
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
		try {
		String bankIds = file.getBANKID();
		String tbatchId = file.getBATCHID();
		String fileName= file.getFILENAME();
		String merId = fileName.substring(0,15);
		String liqDate= fileName.substring(16,16+8);
		 Map<String,Object> resMap = rCkService.reconciliationCheck(bankIds, tbatchId,merId,liqDate);
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
