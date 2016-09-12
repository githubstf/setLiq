package com.buybal.setliq.runnable;

import org.apache.log4j.Logger;

import com.buybal.setliq.service.ComparaMerDataService;
import com.buybal.setliq.service.DownLoadFileService;
import com.buybal.setliq.service.ParseMerFileService;

public class MerReconLiqRunnable implements Runnable {
	private final static Logger logger = Logger.getLogger(MerReconLiqRunnable.class);

	private String remoteFileName = null;
	private String localFileName = null;
	private String merId=null;
	private String liqDate = null;
	private String bankId = null;

	/**
	 * @param merId           平台商户号
	 * @param remoteFileName	远程下载地址
	 * @param localFileName		本地保存地址
	 * @param liqDate			清算对账日期
	 * @param bankId			银行编号
	 */
	public MerReconLiqRunnable(String merId,String remoteFileName,String localFileName,String liqDate,String bankId) {
		this.remoteFileName = remoteFileName;
		this.localFileName = localFileName;
		this.merId=merId;
		this.liqDate=liqDate;
		this.bankId=bankId;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		logger.info("对账/清算处理开始,orgCode="+ this.merId + ",liqDate=" + this.liqDate+",开始时间:"+startTime);
		//远程文件下载保存到本地
		DownLoadFileService downLoadFileService=new DownLoadFileService();
		if (!downLoadFileService.doDownLoadFile(this.localFileName, this.remoteFileName)) {
			logger.info("对账文件下载失败,remoteFileName="+ this.remoteFileName + ",localFileName=" + this.localFileName);
			return;
		}
		
		// 解析对账文件、验签、入库
		ParseMerFileService parseMerFileService=new ParseMerFileService();
		if (!parseMerFileService.doParseFile(this.localFileName,this.liqDate,this.bankId,this.merId)) {
			logger.info("解析对账文件并入库失败,localFileName="+ this.localFileName+",liqDate="+this.liqDate+",bankId="+this.bankId+",merId="+this.merId);
			return;
		}

		// 执行勾兑，做差错处理
		ComparaMerDataService comparaMerDataService=new ComparaMerDataService();
		if (!comparaMerDataService.compareData(this.bankId, this.liqDate, this.merId)) {
			logger.info("勾兑失败,liqDate="+ this.liqDate+",bankId"+this.bankId+",merId="+merId);
			return;
		}
		
		logger.info("对账/清算处理结束,orgCode="+ this.merId + ",liqDate=" + this.liqDate+",总共耗时:"+(System.currentTimeMillis()-startTime));
	}
}

