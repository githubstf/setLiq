import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buybal.epay.dao.DaoConfig;
import com.buybal.epay.dao.TtransactionDAO;
import com.buybal.epay.model.Ttransaction;
import com.buybal.setliq.receivemail.service.ReceiveMailService;
import com.buybal.setliq.receivemail.service.ReconciliationCheckService;
import com.buybal.setliq.receivemail.service.impl.ChinaTvPayReconciliationCheckServiceImpl;
import com.buybal.setliq.receivemail.service.impl.ReceiveMailServiceImpl;
import com.buybal.util.DateUtil;
import com.ccit.ppay.util.StringUtil;
import com.ibatis.dao.client.DaoManager;

public class ReceiveMailServiceTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/* 抓取邮件 */
	@Test
	public void test1() {
		ReceiveMailService service = new ReceiveMailServiceImpl();
		service.receiveAllMail("rytpay@buybal.com", "ryt123", "pop.qq.com",
				110, "0003");
	}

	/*解析入库*/
	@Test
	public void test2() {
		ReconciliationCheckService service = new ChinaTvPayReconciliationCheckServiceImpl();
        service.loadCheckFile("20131123", "0003");
	}
	/*对账*/
	@Test
	public void test3(){
		ReconciliationCheckService service= new ChinaTvPayReconciliationCheckServiceImpl();
		System.out.println(service.reconciliationCheck("0003","","666220088990000", "2013-11-22"));
	}
	
	
	
	
	//对账假数据
	@Test
	public void insert(){
		DaoManager daoManager=	 DaoConfig.getDaoManager();
		TtransactionDAO   dao = (TtransactionDAO) daoManager
				.getDao(TtransactionDAO.class);
		//商户号|终端号|交易时间|交易流水|交易类型|交易金额|手续费|银行卡号|参考号
//		666220088990000|88990000|20131126230706|010340|消费|58300|262|622575******0727|283845283845
//		666220088990000|88990000|20131126231008|010341|消费|8000|36|404738******2407|283859283859
//		666220088990000|88990000|20131126231415|010342|消费|31000|140|622260101******5574|283881283881
//		666220088990000|88990000|20131126235836|010346|消费|12400|56|621785600******2862|283996283996
//		666220088990000|88990000|20131127010126|010348|消费|82000|369|621700239******4096|284065284065
		//金额不平
		Ttransaction ttransaction = new Ttransaction();
		ttransaction.setID("2013091100011112");
		ttransaction.setTBANKID("0003");// 银行 bankId
		ttransaction.setCARD_ACCEPTOR_ID_CODE("666220088990000");// 商户号
		ttransaction.setCARD_ACCEPTOR_TERMINAL_ID("88990000");// 终端号
		ttransaction.setPLATTIME(StringUtil.returnDate("20131126230706"));// 交易日期(yyyyMMdd)
		ttransaction.setTYPE(0);// 交易类型
		ttransaction.setAMT(58301l);// 交易金额
		ttransaction.setRETRIEVAL_REFERENCE_NUMBER("283845283845");// 参考号
		ttransaction.setSYSTEM_TRACE_AUDIT_NUMBER("010340");// 流水号
		ttransaction.setRECON_DATE("2013-11-27");
		ttransaction.setSTATE(1);// 交易成功
		//ttransaction.setBANKCHECK(1);// 对账状态
		dao.insertSelective(ttransaction);
		//金额平
		Ttransaction ttransaction1 = new Ttransaction();
		ttransaction1.setID("2013091100011113");
		ttransaction1.setTBANKID("0003");// 银行 bankId
		ttransaction1.setCARD_ACCEPTOR_ID_CODE("666220088990000");// 商户号
		ttransaction1.setCARD_ACCEPTOR_TERMINAL_ID("88990000");// 终端号
		ttransaction1.setPLATTIME(StringUtil.returnDate("20131126231008"));// 交易日期(yyyyMMdd)
		ttransaction1.setTYPE(0);// 交易类型
		ttransaction1.setAMT(8000l);// 交易金额
		ttransaction1.setRETRIEVAL_REFERENCE_NUMBER("283859283859");// 参考号
		ttransaction1.setSYSTEM_TRACE_AUDIT_NUMBER("010341");// 流水号
		ttransaction1.setRECON_DATE("2013-11-27");
		ttransaction1.setSTATE(1);// 交易成功
		//ttransaction.setBANKCHECK(1);// 对账状态
		dao.insertSelective(ttransaction1);
		//金额平
		Ttransaction ttransaction2 = new Ttransaction();
		ttransaction2.setID("2013091100011114");
		ttransaction2.setTBANKID("0003");// 银行 bankId
		ttransaction2.setCARD_ACCEPTOR_ID_CODE("666220088990000");// 商户号
		ttransaction2.setCARD_ACCEPTOR_TERMINAL_ID("88990000");// 终端号
		ttransaction2.setPLATTIME(StringUtil.returnDate("20131126231415"));// 交易日期(yyyyMMdd)
		ttransaction2.setTYPE(0);// 交易类型
		ttransaction2.setAMT(31000l);// 交易金额
		ttransaction2.setRETRIEVAL_REFERENCE_NUMBER("283881283881");// 参考号
		ttransaction2.setSYSTEM_TRACE_AUDIT_NUMBER("010342");// 流水号
		ttransaction2.setRECON_DATE("2013-11-27");
		ttransaction2.setSTATE(1);// 交易成功
		//ttransaction.setBANKCHECK(1);// 对账状态
		dao.insertSelective(ttransaction2);
		
		System.out.println(StringUtil.returnDate("20131126231417"));
		//平台多出来的数据
		Ttransaction ttransaction3 = new Ttransaction();
		ttransaction3.setID("2013091100011115");
		ttransaction3.setTBANKID("0003");// 银行 bankId
		ttransaction3.setCARD_ACCEPTOR_ID_CODE("666220088990000");// 商户号
		ttransaction3.setCARD_ACCEPTOR_TERMINAL_ID("88990000");// 终端号
		ttransaction3.setPLATTIME(StringUtil.returnDate("20131126231417"));// 交易日期(yyyyMMdd)
		ttransaction3.setTYPE(0);// 交易类型
		ttransaction3.setRECON_DATE("2013-11-27");
		ttransaction3.setAMT(31000l);// 交易金额
		ttransaction3.setRETRIEVAL_REFERENCE_NUMBER("283881283893");// 参考号
		ttransaction3.setSYSTEM_TRACE_AUDIT_NUMBER("010348");// 流水号
		ttransaction3.setSTATE(1);// 交易成功
		//ttransaction.setBANKCHECK(1);// 对账状态
		dao.insertSelective(ttransaction3);
		
	}
	

}
