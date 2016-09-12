package com.buybal.setliq.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.buybal.setliq.service.Reconciliation2002Service;

public class Recon2002Job extends QuartzJobBean {
	Logger logger = LoggerFactory.getLogger(Recon2002Job.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		logger.info("Recon2002Job start");
		Reconciliation2002Service service = new Reconciliation2002Service();
	    service.receiveMail();
		service.loadFile("2002");
		logger.info("Recon2002Job end");
	}

	
	public static void main(String[] args) {
		Reconciliation2002Service service = new Reconciliation2002Service();
		service.receiveMail();
		service.loadFile("2002");
	}
}
