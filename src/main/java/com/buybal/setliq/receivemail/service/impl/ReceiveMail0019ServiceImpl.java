package com.buybal.setliq.receivemail.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buybal.epay.model.Tbank;
import com.buybal.epay.model.Tbankchkfile;
import com.buybal.epay.model.Tdatadic;
import com.buybal.epay.service.TbankService;
import com.buybal.epay.service.TbankchefileService;
import com.buybal.epay.service.TseqService;
import com.buybal.epay.service.common.DataDicService;
import com.buybal.setliq.receivemail.service.ReceiveMailService;
import com.buybal.util.PropertiseUtil;
import com.buybal.util.StringUtil;

//import org.springframework.transaction.annotation.Transactional;

public class ReceiveMail0019ServiceImpl implements ReceiveMailService {

	private static Logger logger = LoggerFactory.getLogger(ReceiveMail0019ServiceImpl.class);

	private TbankchefileService service = new TbankchefileService();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private int count = 0;
	StringBuffer bodytext = new StringBuffer();// 存放邮件内容

	private String flag;

	@Override
	public int receiveAllMail(String userName, String passWord, String mailHost, Integer port, String bankId) {
		// 获取全部接收邮箱
		if (count != 0) {
			return 0;
		}
		try {
			count = 1;
			receiveMail(userName, passWord, mailHost, port, bankId);

		} catch (Exception e) {
			logger.error("exception", e);
		} finally {
			count = 0;
		}

		return 1;
	}

	// 验证邮箱账号密码
	public boolean validateMail(String userName, String passWord, String mailHost, Integer port) {
		Store store = null;
		Properties prop = new Properties();
		String type = "";
		if (mailHost.indexOf("imap") != -1) {
			type = "imap";
		} else {
			type = "pop3";
		}
		prop.put("mail." + type + ".host", mailHost);
		if (null != port) {// 端口号 pop3默认是110，imap默认是25
			prop.put("mail." + type + ".port", String.valueOf(port));
		}

		prop.put("mail.store.protocol", type);
		Session session = Session.getInstance(prop);

		try {
			store = session.getStore(type);
			store.connect(userName, passWord);
		} catch (NoSuchProviderException e) {
			return false;
		} catch (MessagingException e) {
			return false;
		} finally {
			if (store != null && store.isConnected()) {
				try {
					store.close();
				} catch (MessagingException e) {
				}
			}
		}

		return true;
	}

	/**
	 * 接收邮件
	 * 
	 * @param 邮箱的用户名和密码
	 * @return 无
	 */
	private void receiveMail(String userName, String passWord, String mailHost, Integer port, String bankId) {

		Folder folder = null;
		Store store = null;

		Properties prop = new Properties();
		String type = "";
		if (mailHost.indexOf("imap") != -1) {
			type = "imap";
		} else {
			type = "pop3";
		}
		prop.put("mail." + type + ".host", mailHost);
		if (null != port) {// 端口号 pop3默认是110，imap默认是25
			prop.put("mail." + type + ".port", String.valueOf(port));
		}

		prop.put("mail.store.protocol", type);
		Session session = Session.getInstance(prop);

		try {
			store = session.getStore(type);
			store.connect(userName, passWord);
			folder = store.getFolder("INBOX"); // 收件箱
			folder.open(Folder.READ_ONLY);
			if (folder.getMessageCount() == 0) {
				return;
			}

			Date nowDate = new Date();
			Message[] messages = folder.getMessages();
			for (int i = messages.length - 1; i >= 0; i--) {
				Message message = messages[i];

				// for (Message message : messages) {
				Map<String, Object> map = null;
				Date sentDate = message.getSentDate();// 获取发送时间

				if (!sdf.format(sentDate).equals(sdf.format(nowDate))) {
					break;
				}

				StringBuffer sb = new StringBuffer();
				Enumeration en = message.getAllHeaders();
				while (en.hasMoreElements()) { // 获取主题
					Header header = (Header) en.nextElement();
					if (header.getName().equalsIgnoreCase("Subject")) {
						sb.append(header.getValue());
					}
				}
				String subject = sb.toString();
				subject = MimeUtility.decodeText(subject.trim());// 将邮件主题解码

				String content = getContent((Part) message);// 获取正文
				bodytext = new StringBuffer();

				try {
					if (isContainAttach(message)) {// 判断是否有附件
						DataDicService dds = new DataDicService();
						Tdatadic Tdatadic = dds.getDataDic("FILE_URL", "sharefile");
						if (Tdatadic == null) {
							logger.error("字典表文件存放路径参数不存在FILE_URL+sharefile");
							return;
						}
						String filePath = Tdatadic.getDIC_NAME() + "bankRecon/" + bankId + "/";
						map = saveAttachMent(message, filePath, bankId);
					} else {
						continue;
					}
					map.put("bankId", bankId);
				} catch (Exception e) {
					logger.error("上传附件失败", e);
					i++;// 如果上传附件失败，再重新抓取
					continue;
				}
				map.put("subject", subject);
				map.put("content", content);
				map.put("sentDate", sentDate);// newName,size,fileName

				try {
					saveMail(map);
				} catch (Exception e) {
					logger.error("邮件入库异常", e);
					i++;// 如果入库失败，再重新抓取
					continue;
				}
			}// end for
		} catch (NoSuchProviderException e) {
			logger.error("exception", e);
		} catch (MessagingException e1) {
			logger.error("登录邮箱失败      " + userName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			if (folder != null && folder.isOpen()) {
				try {
					folder.close(true);
				} catch (MessagingException e) {
				}
			}
			if (store != null) {
				if (store.isConnected()) {
					try {
						store.close();
					} catch (MessagingException e) {
					}
				}
			}

		}

	}

	/**
	 * 判断公司代码是否存在
	 * 
	 * @return
	 */
	private boolean validateSubject(String subject) {
		if (StringUtil.isEmpty(subject)) {
			return false;
		}
		if (subject.split("\\*").length != 2) {
			return false;
		}

		// if (resInfoDbService.selectByEmailTitle(subject).size() == 0) {
		// return true;
		// }
		return false;
	}

	/**
	 * 处理邮件正文的html代码
	 * 
	 * @param str
	 * @return
	 */
	private String filterHtml(String regxpForHtml, String str) {
		Pattern pattern = Pattern.compile("<([^>]*)>");
		Matcher matcher = pattern.matcher(str);
		StringBuffer sb = new StringBuffer();
		boolean result1 = matcher.find();
		while (result1) {
			matcher.appendReplacement(sb, "");
			result1 = matcher.find();
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 获取邮件内容
	 * 
	 * @param part
	 *            ：Part
	 */

	private String getMailContent(Part part) {
		try {
			if (part.isMimeType("text/plain")) {
				bodytext.append((String) part.getContent());
			} else if (part.isMimeType("text/html")) {
				bodytext.append((String) part.getContent());
			} else if (part.isMimeType("multipart/*")) {
				Multipart multipart = (Multipart) part.getContent();
				int counts = multipart.getCount();
				for (int i = 0; i < counts; i++) {
					getMailContent(multipart.getBodyPart(i));
				}
			} else if (part.isMimeType("message/rfc822")) {
				getMailContent((Part) part.getContent());
			} else {
			}
		} catch (MessagingException e) {
			logger.error("exception", e);
		} catch (IOException e) {
			logger.error("exception", e);
		}
		return bodytext.toString();
	}

	private String getContent(Part part) {
		try {
			if (part.isMimeType("multipart/*")) {
				Multipart p = (Multipart) part.getContent();
				int count = p.getCount();
				if (count > 1)
					count = 1;
				for (int i = 0; i < count; i++) {
					BodyPart bp = p.getBodyPart(i);
					getContent(bp);
				}
			} else if (part.isMimeType("text/*")) {
				bodytext.append(part.getContent());
			}
		} catch (MessagingException e) {
			logger.error("exception", e);
		} catch (IOException e) {
			logger.error("exception", e);
		}
		return bodytext.toString();
	}

	/**
	 * 判断此邮件是否包含附件
	 * 
	 * @param part
	 *            ：Part
	 * @return 是否包含附件
	 */
	private boolean isContainAttach(Part part) throws Exception {
		boolean attachflag = false;
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE))))
					attachflag = true;
				else if (mpart.isMimeType("multipart/*")) {
					attachflag = isContainAttach((Part) mpart);
				} else {
					String contype = mpart.getContentType();
					if (contype.toLowerCase().indexOf("application") != -1)
						attachflag = true;
					if (contype.toLowerCase().indexOf("name") != -1)
						attachflag = true;
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			attachflag = isContainAttach((Part) part.getContent());
		}
		return attachflag;
	}

	/**
	 * 判断此邮件是否包含pdf附件
	 * 
	 * @param part
	 *            ：Part
	 * @return 是否包含附件
	 */
	private boolean isContainAttachPdf(Part part) throws Exception {
		String fileName = "";
		boolean attachflag = false;
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				fileName = mpart.getFileName();
				if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) {
					fileName = MimeUtility.decodeText(fileName);
					if ("pdf".equals(fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length()).toLowerCase())) {
						attachflag = true;
					}
				} else if (mpart.isMimeType("multipart/*")) {
					attachflag = isContainAttachPdf((Part) mpart);
				} else {
					String contype = mpart.getContentType();
					if (contype.toLowerCase().indexOf("application") != -1)
						fileName = MimeUtility.decodeText(fileName);
					if (fileName != null && "pdf".equals(fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length()).toLowerCase())) {
						attachflag = true;
					}
					if (contype.toLowerCase().indexOf("name") != -1)
						fileName = MimeUtility.decodeText(fileName);
					if (fileName != null && "pdf".equals(fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length()).toLowerCase())) {
						attachflag = true;
					}
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			attachflag = isContainAttachPdf((Part) part.getContent());
		}
		return attachflag;
	}

	/**
	 * 保存附件
	 * 
	 * @param part
	 *            ：Part
	 * @param filePath
	 *            ：邮件附件存放路径
	 */
	private Map<String, Object> saveAttachMent(Part part, String filePath, String bankId) throws Exception {
		List<String> nameList = new ArrayList<String>();
		String fileName = "";
		Map<String, Object> map = null;
		// 保存附件到服务器本地
		String startsName=PropertiseUtil.getString("setLiq", "ORG_0019");
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) {
					fileName = mpart.getFileName();
					if (fileName != null) {
						
						if (!fileName.startsWith(startsName)) {
							map = new HashMap<String, Object>();
							logger.info("附件名称错误fileName="+fileName);
							return map;
						}
						map = saveFile(fileName, mpart.getInputStream(), filePath);
						nameList.add(fileName);

						map.put("nameList", nameList);
						map.put("fileName", fileName);
					}
				} else if (mpart.isMimeType("multipart/*")) {
					map = saveAttachMent(mpart, filePath, bankId);
				} else {
					fileName = mpart.getFileName();
					if (fileName != null) {
						if (!fileName.startsWith(startsName)) {
							map = new HashMap<String, Object>();
							logger.info("附件名称错误fileName="+fileName);
							return map;
						}
						fileName = MimeUtility.decodeText(fileName);
						map = saveFile(fileName, mpart.getInputStream(), filePath);
						map.put("fileName", fileName);
					}
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			saveAttachMent((Part) part.getContent(), filePath, bankId);
		}

		return map;
	}

	/**
	 * 保存附件到指定目录里
	 * 
	 * @param fileName
	 *            ：附件名称
	 * @param in
	 *            ：文件输入流
	 * @param filePath
	 *            ：邮件附件存放基路径
	 */
	private Map<String, Object> saveFile(String fileName, InputStream in, String filePath) throws Exception {

		Map<String, Object> map = new HashMap<String, Object>();

		File storefile = new File(filePath);
		if (!storefile.exists()) {
			storefile.mkdirs();
		}

		File file = new File(filePath, fileName);

		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;

		bos = new BufferedOutputStream(new FileOutputStream(file));
		bis = new BufferedInputStream(in);
		int c;
		try {
			while ((c = bis.read()) != -1) {
				bos.write(c);
				bos.flush();
			}
		} catch (Exception e) {
			if (file.exists()) {
				file.delete();
			}
			throw e;
		} finally {
			if (bos != null) {
				bos.close();
			}
			if (bis != null) {
				bis.close();
			}
		}

		map.put("newName", fileName);
		map.put("size", (file.length() / 1024) + "");

		return map;
	}

	/**
	 * 入库
	 * 
	 * @param fileName
	 *            ：附件名称
	 * @param filePath
	 *            ：邮件附件存放基路径
	 * @throws UnsupportedEncodingException
	 * 
	 */
	private int saveMail(Map<String, Object> map) throws UnsupportedEncodingException {
		List<String> files = (List<String>) map.get("nameList");
		if (files == null || files.size() < 1) {
			return 0;
		}
		for (int i = 0; i < files.size(); i++) {
			Tbankchkfile tbankchkfile = new Tbankchkfile();
			String fileName = files.get(i);
			String bankId = (String) map.get("bankId");
			fileName = MimeUtility.decodeText(fileName);
			tbankchkfile.setBANKID(bankId);
			tbankchkfile.setFILENAME(fileName);
			int a = service.selectTbankchkfile(tbankchkfile);
			if (a > 0) {
				logger.info("已经入库fileName="+fileName+"，不能重复入库");
				continue;
			}
			Date date = new Date();
			TseqService ts = new TseqService();
			tbankchkfile.setBATCHID(ts.getAccBatchId());
			DataDicService dds = new DataDicService();
			Tdatadic Tdatadic = dds.getDataDic("FILE_URL", "sharefile");
			if (Tdatadic == null) {
				logger.error("字典表文件存放路径参数不存在FILE_URL+sharefile");
			}
			String filePath = Tdatadic.getDIC_NAME() + "bankRecon/" + bankId + "/";
			tbankchkfile.setFILEPATH(filePath);
			TbankService bankService = new TbankService();
			Tbank bank = bankService.selectByPrimaryKey(bankId);
			String bankName = bank.getNAME();
			tbankchkfile.setPLATTIME(date);
			tbankchkfile.setBANKNAME(bankName);
			tbankchkfile.setSTATE(0);
			service.deleteByKey(bankId, fileName);// 先删除记录
			service.insertSelective(tbankchkfile);
			logger.info("下载附件成功");
		}
		return 1;
	}

}
