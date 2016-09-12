package com.buybal.setliq.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import com.buybal.setliq.service.UmpayReconciliationService;
/**
 * 联动优势对账定时程序
 * @author wxw
 *
 */
public class UmpayReconJob extends QuartzJobBean{
	
	Logger logger = LoggerFactory.getLogger(UmpayReconJob.class);

	@Override
	protected void executeInternal(JobExecutionContext arg0)
			throws JobExecutionException {
		logger.info("UmpayReconJob start");
		UmpayReconciliationService service = new UmpayReconciliationService();
		service.downLoadFile();
		service.loadFile("UMPAY");
		logger.info("UmpayReconJob end");
		
	}

}
