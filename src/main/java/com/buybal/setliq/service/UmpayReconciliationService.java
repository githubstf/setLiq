package com.buybal.setliq.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.buybal.epay.model.Tbank;
import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.service.TbankService;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.setliq.file.service.ReconciliationCheckService;
import com.buybal.setliq.file.service.impl.UmpayReconciliationCheckService;
import com.buybal.setliq.receivemail.service.DownLoadFileService;
import com.buybal.setliq.receivemail.service.impl.UmpayDownLoadFileServiceImpl;


public class UmpayReconciliationService {
	
	private static final Logger logger = Logger.getLogger(UmpayReconciliationService.class);
	
	private TbankchefileService bcf = new TbankchefileService();
	private ReconciliationCheckService rCkService = new UmpayReconciliationCheckService();

	/**
	 * 下载银行对账文件
	 */
	public void downLoadFile() {
		logger.info("下载对账文件");
		DownLoadFileService service = new UmpayDownLoadFileServiceImpl();
		service.downLoadFile();
	}

	// 加载
	public void loadFile(String bankId) {
		Map<String,Object> map = new HashMap();
	    map.put("bankId", bankId);
	    map.put("state", 0);
	    //查询初始状态
		List<Tbankchkfile> files = bcf.getTbankchkfileListByBankId(map);
		if (files.size() < 1) {
			logger.info("没有需要加载的对账文件！bankId="+bankId);
             return;
		}
		for (Tbankchkfile file : files) {
			String tbatchId = file.getBATCHID();
			Map<String,Object> resMap = rCkService.loadCheckFile(tbatchId, bankId);//读取对账文件List   入tbankdetail
			if(!"0000".equals(resMap.get("errorCode"))){
				logger.info("加载失败bankId="+bankId+",tbatchId="+tbatchId+",error="+resMap.get("errorMsg"));
				continue;
			}
			reconciliationCheck(file);//平台与银行的对账
		}
	}

	// 对账
	private int reconciliationCheck(Tbankchkfile file) {
		try {
		String bankIds = file.getBANKID();
		String tbatchId = file.getBATCHID();
		String fileName= file.getFILENAME();
		Tbank bank=new TbankService().selectByPrimaryKey(bankIds);
		String merId = bank.getMERID();
		String liqDate= fileName.substring(0,8);//yyyyMMdd
		Map<String,Object> resMap = rCkService.reconciliationCheck(bankIds, tbatchId,merId,liqDate);
			 if(!"0000".equals(resMap.get("retCode"))){
				logger.info("对账失败bankId="+bankIds+",tbatchId="+tbatchId+",error="+resMap.get("retMsg"));
				 return 0;
			 }
		} catch (Exception e) {
			logger.error("对账异常！",e);
			return 0;
		}
		return 1;
	}

}
