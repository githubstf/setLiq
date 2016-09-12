package com.buybal.setliq.runnable;

import org.apache.log4j.Logger;

import com.buybal.setliq.service.ComparaOrgDataService;
import com.buybal.setliq.service.DownLoadFileService;
import com.buybal.setliq.service.ParseOrgFileService;

public class OrgReconLiqRunnable implements Runnable {
	private final static Logger logger = Logger.getLogger(OrgReconLiqRunnable.class);

	private String remoteFileName = null;
	private String localFileName = null;
	private String orgCode=null;
	private String liqDate = null;
	private String bankId = null;

	/**
	 * @param orgCode           buybal平台机构号
	 * @param remoteFileName	远程下载地址
	 * @param localFileName		本地保存地址
	 * @param liqDate			清算对账日期
	 * @param bankId			银行编号
	 */
	public OrgReconLiqRunnable(String orgCode,String remoteFileName,String localFileName,String liqDate,String bankId) {
		this.remoteFileName = remoteFileName;
		this.localFileName = localFileName;
		this.orgCode=orgCode;
		this.liqDate=liqDate;
		this.bankId=bankId;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		logger.info("对账/清算处理开始,orgCode="+ this.orgCode + ",liqDate=" + this.liqDate+",开始时间:"+startTime);
		//远程文件下载保存到本地
		DownLoadFileService downLoadFileService=new DownLoadFileService();
		if (!downLoadFileService.doDownLoadFile(this.localFileName, this.remoteFileName)) {
			logger.info("对账文件下载失败,remoteFileName="+ this.remoteFileName + ",localFileName=" + this.localFileName);
			return;
		}
		
		// 解析对账文件、验签、入库
		ParseOrgFileService parseOrgFileService=new ParseOrgFileService();
		if (!parseOrgFileService.doParseFile(this.localFileName,this.liqDate,this.bankId)) {
			logger.info("解析对账文件并入库失败,localFileName="+ this.localFileName+",liqDate="+this.liqDate+",bankId="+this.bankId);
			return;
		}

		// 执行勾兑，做差错处理
		ComparaOrgDataService comparaDataService=new ComparaOrgDataService();
		if (!comparaDataService.compareData(this.bankId, this.liqDate)) {
			logger.info("勾兑失败,liqDate="+ this.liqDate+",bankId"+this.bankId);
			return;
		}
		
		logger.info("对账/清算处理结束,orgCode="+ this.orgCode + ",liqDate=" + this.liqDate+",总共耗时:"+(System.currentTimeMillis()-startTime));
	}
}

