package com.buybal.setliq.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.buybal.epay.model.Tbankdetail;
import com.buybal.epay.service.TbankdetailService;
import com.buybal.util.AmountUtil;
import com.buybal.util.DateUtil;
import com.buybal.util.MD5Encryptor;

/**
 * 
 * @ClassName: ParseFileService
 * @Description: 商户对数据进行解析验签和入库
 * @author Hippo
 * @date 2014-9-19
 * @version 1.0
 */
public class ParseMerFileService {
	private static final Logger logger = Logger.getLogger(ParseMerFileService.class);

	private int purCnt = 0; // 交易笔数
	private long purAmt = 0; // 交易金额
	private long feeAmt = 0; // 手续费金额

	/**
	 * @Description: 对下载到本地的数据进行解析、验签并入库
	 * @param localFileName 本地文件存储的路径
	 * @param liqDate 清算日期
	 * @param bankId 银行编号
	 * @param merId 商户编号
	 * @return boolean 返回类型 解析成功为true 解析失败为 false
	 */
	public boolean doParseFile(String localFileName, String liqDate,String bankId,String merId) {
		logger.info("[文件解析Service]开始解析对账文件");
		if (StringUtils.isEmpty(localFileName)) {
			logger.error("[文件解析Service]参数错误,localFileName=" + localFileName);
			return false;
		}
		if (StringUtils.isEmpty(liqDate)) {
			logger.error("[文件解析Service]参数错误,liqDate=" + liqDate);
			return false;
		}
		if (StringUtils.isEmpty(bankId)) {
			logger.error("[文件解析Service]参数错误,bankId=" + bankId);
			return false;
		}
		if (StringUtils.isEmpty(merId)) {
			logger.error("[文件解析Service]参数错误,merId=" + merId);
			return false;
		}
		File file = new File(localFileName);
		String lineData;// 文件主体一行信息
		String header;// 文件头信息
		BufferedReader bufferReader = null;
		if (!file.exists()) {
			logger.error("[文件解析Service]错误文件路径：" + localFileName);
			return false;
		}
		try {
			bufferReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			// 获取头文件信息
			header = bufferReader.readLine(); //开始日期|结束日期|机构编号|总交易笔数|总交易金额|总手续费|签名串
			if (header == null) {
				logger.error("[文件解析Service]错误文件路径：" + localFileName);
				return false;
			}
			bufferReader.mark((int) file.length() + 1);
			String[] heads = header.split("\\|");
			if(heads == null || heads.length != 7){
				logger.error("[文件解析Service]头部文件格式异常：header=" + header);
				return false;
			}
			// 对每行进行验签
			while ((lineData = bufferReader.readLine()) != null) {
				if (!verifySign(merId, lineData)) {
					logger.error("[文件解析Service]数据信息签名错误");
					return false;
				}
			}
			// 验证头信息和行信息总和是否匹配
			if (this.purCnt != Integer.valueOf(heads[3])) {
				logger.error("[文件解析Service]头部总信息与行信息总交易笔数不匹配,头部数="+heads[3]+",实际数="+purCnt);
				return false;
			}
			if (this.purAmt != Long.parseLong(heads[4])) {
				logger.error("[文件解析Service]头部总信息与行信息总交易金额不匹配,头部数="+heads[4]+",实际数="+purAmt);
				return false;
			}
			if (this.feeAmt != Long.parseLong(heads[5])) {
				logger.error("[文件解析Service]头部总信息与行信息总手续费不匹配,头部数="+heads[5]+",实际数="+feeAmt);
				return false;
			}

			// 为解决可以重复发送对账，删除前面可能入库的数据
			TbankdetailService bankdetailService = new TbankdetailService();
			Map<String , Object> map = new HashMap<String , Object>();
			map.put("BANKID",  bankId);
			map.put("LIQ_DATE",  liqDate);
			map.put("MER_ID",  merId);
			int rows = bankdetailService.deleteBySelective(map);
			logger.debug("删除的记录数rows="+rows);
			
			// 对账文件数据入库
			bufferReader.reset();
			while ((lineData = bufferReader.readLine()) != null) {
				if (!insertToDB(lineData,bankId)) {
					logger.error("[文件解析Service]文件入库失败");
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("[文件解析Service]错误文件路径：" + localFileName, e);
			return false;
		} finally {
			try {
				if (bufferReader != null) {
					bufferReader.close();
				}
			} catch (IOException e) {
				logger.error("[文件解析Service]关闭流失败", e);
			}
		}
		return true;
	}

	/**
	 * 
	 * @param merId 商户编号
	 * @param lineData
	 * 商户号|终端号|批次号|流水号|授权码|检索参考号|交易时间(yyyyMMddHHmmss)|清算日期(yyyyMMdd)|卡号|交易金额(分)|交易手续费(分)|交易类型|原交易检索参考号|商户订单号|签名串
	 * @return
	 */
	private boolean verifySign(String merId, String lineData) {
		purCnt++;
		String[] dataArray = lineData.split("\\|");
		if(dataArray == null || dataArray.length != 15){
			logger.error("[文件解析Service]第"+purCnt+"行数据格式异常");
			return false;
		}
		String signData = dataArray[14]; //签名串
		//TODO 验证签名
		purAmt += Long.parseLong(dataArray[9]) ;
		feeAmt += Long.parseLong(dataArray[10]) ;
		return true;
	}

	/**
	 * 解析文件入库
	 * @param lineData
	 * @return
	 */
	private boolean insertToDB(String lineData,String bankId) {
		String[] str = lineData.split("\\|");
		Tbankdetail bd = new Tbankdetail();
		bd.setBANKID(bankId);
		bd.setBANKNAME("");
		//商户号|终端号|批次号|流水号|授权码|检索参考号|交易时间(yyyyMMddHHmmss)|清算日期(yyyyMMdd)|
		//卡号|交易金额(分)|交易手续费(分)|交易类型|原交易检索参考号|商户订单号|签名串
		String bankOrderId = MD5Encryptor.MD5Encode(str[0] + str[1] + str[2] + str[3] + str[5] + str[6]);// 银行订单号=MD5(商户号+终端号+批次号+交易流水+参考号+交易时间)
		bd.setBANKORDERID(bankOrderId); // 银行订单号
		bd.setTRANS_TYPE_STR("");// 交易类型说明
		bd.setMER_ID(str[0]); // 商户号
		bd.setPOS_ID(str[1]); // 终端号
		bd.setBATCHID(str[2]); //批次号
		bd.setSEQ_NO(str[3]); // 交易流水
		bd.setREF_NO(str[5]); // 参考号
		bd.setBANKORDERTIME(DateUtil.StringToDate(str[6], "yyyyMMddHHmmss"));// 交易时间
		bd.setLIQ_DATE(str[7]); // 清算日期(yyyyMMdd)
		bd.setCARD_NO(str[8]);// 银行卡号
		bd.setTRANSACTIONAMT(Long.parseLong(str[9]));// 交易金额
		bd.setFEE_AMT(Long.parseLong(str[10])); // 手续费
		bd.setTRANSACTIONTYPE(Integer.parseInt(str[11])); //交易类型
		bd.setORI_REF_NO(str[12]); // 原始凭证号
		bd.setMER_TYPE("0");// 商户类型
		bd.setMER_NAME("");// 商户名称
		bd.setCHECKDATE(new Date());
		// bd.setORI_SEQ_ID(); 原始流水号
		// bd.setCARD_TYPE_STR(); 卡类型描述
		
		// 信息入库
		TbankdetailService bankdetailService = new TbankdetailService();
		try{
			bankdetailService.insertSelective(bd);
		}catch(Exception e){
			logger.error("入库失败lineData="+lineData+",bankId="+bankId,e);
		}
		return true;
	}

}
