package com.buybal.setliq.receivemail.service.impl;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.service.TbankService;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.epay.service.TseqService;
import com.buybal.epay.service.common.GeneralService;
import com.buybal.epay.service.files.UmpayChecFileService;
import com.buybal.epay.util.Constant;
import com.buybal.setliq.receivemail.service.DownLoadFileService;
import com.buybal.util.DateUtil;

public class UmpayDownLoadFileServiceImpl implements DownLoadFileService{
	
	private static final Logger logger = Logger.getLogger(UmpayDownLoadFileServiceImpl.class);

	private UmpayChecFileService checkFileService = new UmpayChecFileService();
	private TbankchefileService service = new TbankchefileService();
	/**
	 *   return 1成功  0失败
	 */
	public int downLoadFile() {
		logger.info("下载对账文件");
		Date date = new Date();
		String settleDate = DateUtil.fomatDate(DateUtil.getAnyDayByNo(date, -1), "yyyyMMdd");//获取前一天的对账文件
		//调对账接口
		Map<String, String> map = checkFileService.doBusiness(settleDate);
		String retCode = map.get("retCode");
		if(!"0000".equals(retCode)) {
			return 0;
		}
		//向tbankchkfile录入
		int result = saveCheckFile(map);
		return result;
	}
	
	/**
	 * 入库
	 * @param map
	 * @return 1成功  0失败
	 */
	private int saveCheckFile(Map<String,String> map) {
		Tbankchkfile tbankchkfile = new Tbankchkfile();
		String bankId = "UMPAY";
		String fileName = map.get("fileName"); 
		String filePath = map.get("filePath");
		
		tbankchkfile.setBANKID(bankId);
		tbankchkfile.setFILENAME(fileName);
		int a = service.selectTbankchkfile(tbankchkfile);
		if (a > 0) {
			logger.info("已经入库，不能重复入库");
			return 0;
		}
		Date date = new Date();
		TseqService ts = new TseqService();
		tbankchkfile.setBATCHID(ts.getAccBatchId());
		TbankService bankService = new TbankService();
//		Tbank bank = bankService.selectByPrimaryKey(bankId);
		tbankchkfile.setFILEPATH(filePath);
//		String bankName = bank.getNAME();
		tbankchkfile.setPLATTIME(date);
//		tbankchkfile.setBANKNAME(bankName);
		tbankchkfile.setSTATE(0);
		tbankchkfile.setBANKNAME("联动优势");
		service.deleteByKey(bankId, fileName);// 先删除记录
		service.insertSelective(tbankchkfile);
		logger.info("上传对账文件成功");
		GeneralService.createTweblogForMgr("", "上传对账文件成功",Constant.LOG_LEVEL_INFO, Constant.OP_TYPE_DZMANAMGER,null);
		return 1;
	}
	
	public static void main(String[] args) {
		new UmpayDownLoadFileServiceImpl().downLoadFile();
	}

}
