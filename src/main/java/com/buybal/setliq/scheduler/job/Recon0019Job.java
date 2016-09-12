package com.buybal.setliq.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import com.buybal.setliq.service.Reconciliation0019Service;

public class Recon0019Job extends QuartzJobBean {
	Logger logger = LoggerFactory.getLogger(Recon0019Job.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		logger.info("Recon0019Job start");
		Reconciliation0019Service service = new Reconciliation0019Service();
	    service.receiveMail();
		service.loadFile("0019");
		logger.info("Recon0019Job end");
	}

	
	public static void main(String[] args) {
		Reconciliation0019Service service = new Reconciliation0019Service();
	    service.receiveMail();
		service.loadFile("0019");
	}

}
