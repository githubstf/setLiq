package com.buybal.setliq.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.buybal.epay.dao.DaoConfig;
import com.buybal.epay.dao.TReconErrDAO;
import com.buybal.epay.dao.TbankDAO;
import com.buybal.epay.dao.TbankchkfileDAO;
import com.buybal.epay.dao.TbankchkresultDAO;
import com.buybal.epay.dao.TbankdetailDAO;
import com.buybal.epay.dao.TtransactionDAO;
import com.buybal.epay.model.TReconErr;
import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.service.TseqService;
import com.ibatis.dao.client.DaoManager;


/**
 * @Description: 比对对账文件数据和本地数据库数据，并做差错处理
 * @author Hippo
 * @date 2014-9-14
 * @version 1.0
 */
public class ComparaMerDataService {
	private static final Logger logger = Logger.getLogger(ComparaMerDataService.class);
	private DaoManager daoManager;
	private TbankdetailDAO tbankdetailDAO;
	private TbankchkfileDAO tbankchkfileDAO;
	private TbankDAO tbankDAO;
	private TbankchkresultDAO tbankchkresultDAO;
	private TtransactionDAO transactionDAO;
	private TReconErrDAO tReconErrDAO;
	private TseqService seqService = null;

	public ComparaMerDataService() {
		daoManager = DaoConfig.getDaoManager();
		tbankdetailDAO = (TbankdetailDAO) daoManager.getDao(TbankdetailDAO.class);
		tbankchkfileDAO = (TbankchkfileDAO) daoManager.getDao(TbankchkfileDAO.class);
		tbankDAO = (TbankDAO) daoManager.getDao(TbankDAO.class);
		tbankchkresultDAO = (TbankchkresultDAO) daoManager.getDao(TbankchkresultDAO.class);
		transactionDAO = (TtransactionDAO) daoManager.getDao(TtransactionDAO.class);
		tReconErrDAO = (TReconErrDAO) daoManager.getDao(TReconErrDAO.class);
		seqService = new TseqService();
	}


	/**
	 * 对账处理(tbankdetail与ttransaction做比较) 
	 * 1.删除对账结果表记录 
	 * 2.删除对账明细7七天前交易 
	 * 3.去掉金额相等条件,设置所有对账状态为2,金额不相等
	 * 4.再加上金额相等条件，设置所有对账状态为1,对账成功(对账明细条件为未对账) 
	 * 5.标记对账明细表对账成功的为已对账
	 * 6.查询交易表未对账成功交易入库结果表为平台多交易 
	 * 7.查询
	 */
	public boolean compareData(String bankId, String liqDate,String merId) {
		logger.info("平台机构对账开始bankId=" + bankId + ",liqDate=" + liqDate+",merId="+merId);
		TReconErr tr = new TReconErr();

		tr.setBANK_ID(bankId);
		tr.setRECON_DATE(liqDate);
		tr.setSTATE(0);
		tr.setREG_TIME(new Date());

		Tbankchkfile upDateBankche = new Tbankchkfile();
		upDateBankche.setBANKID(bankId);

		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("BANKID", bankId);
		paramMap.put("LIQ_DATE", liqDate);
		paramMap.put("MER_ID", merId);

		// 去掉金额相等条件,设置所有对账状态为2,金额不相等
		paramMap.put("TABAL_TAG", "ttransaction"); // 更新的表ttransaction
		paramMap.put("AMT_TAG", "0"); // 0不增加金额条件
		paramMap.put("CHECK_STATE", 2); // 2金额不相等
		int rows = tbankdetailDAO.updateCheckState(paramMap);

		// 再加上金额相等条件，设置所有对账状态为1,对账成功
		paramMap.put("TABAL_TAG", "ttransaction"); // 更新的表ttransaction
		paramMap.put("AMT_TAG", "1"); // 1则增加金额相等条件
		paramMap.put("CHECK_STATE", 1); // 1平帐
		rows = tbankdetailDAO.updateCheckState(paramMap);

		// 标记对账明细表对账成功的为已对账
		paramMap.put("TABAL_TAG", "tbankdetail"); // 更新的表tbankdetail
		paramMap.put("AMT_TAG", "0"); // 如果等于1则增加金额相等条件
		paramMap.put("CHECK_STATE", 1); // 1平帐,2金额不相等
		rows = tbankdetailDAO.updateCheckState(paramMap);

		// 删除对账结果表数据
		rows = tbankchkresultDAO.deleteResByCheck(paramMap);
		logger.debug("deleteResByCheck.rows="+rows);

		// 查询金额不平(即ttransaction对账状态为2)
		rows = tbankchkresultDAO.insertAmtNotEqualData(paramMap);
		logger.debug("insertAmtNotEqualData.rows="+rows);
		 
		 // 查询平台多(即ttransaction对账状态为0)
		rows = tbankchkresultDAO.insertPlatMoreData(paramMap);
		logger.debug("insertPlatMoreData.rows="+rows);
		
		// 查询银行多(即tbankdetail对账状态为0)
		rows = tbankchkresultDAO.insertBankMoreData(paramMap);
		logger.debug("insertBankMoreData.rows="+rows);

		upDateBankche.setSTATE(1); // 已对账
		tbankchkfileDAO.updateByPrimaryKeySelective(upDateBankche);

		logger.info("平台机构对账结束bankId=" +bankId+ ",liqDate=" + liqDate+",merId="+merId);
		return true;

	}

}
