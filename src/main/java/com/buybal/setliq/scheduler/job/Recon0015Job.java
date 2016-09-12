package com.buybal.setliq.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.buybal.setliq.service.Reconciliation0015Service;

public class Recon0015Job extends QuartzJobBean {
	Logger logger = LoggerFactory.getLogger(Recon0015Job.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		logger.info("Recon0015Job start");
		Reconciliation0015Service service = new Reconciliation0015Service();
	    service.receiveFtp();
		service.loadFile("0015");
		logger.info("Recon0015Job end");
	}

public static void main(String[] args) {
	Reconciliation0015Service service = new Reconciliation0015Service();
    service.receiveFtp();
	service.loadFile("0015");
}
}
