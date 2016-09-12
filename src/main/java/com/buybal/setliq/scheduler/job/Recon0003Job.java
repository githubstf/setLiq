package com.buybal.setliq.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.buybal.setliq.service.ReconciliationService;

public class Recon0003Job extends QuartzJobBean {
	Logger logger = LoggerFactory.getLogger(Recon0003Job.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		logger.info("Recon0003Job start");
		ReconciliationService service = new ReconciliationService();
		service.receiveMail();
		service.loadFile("0003");
		logger.info("Recon0003Job end");
	}

}
