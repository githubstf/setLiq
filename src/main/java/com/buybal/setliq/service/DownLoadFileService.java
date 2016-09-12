package com.buybal.setliq.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * @ClassName: DownLoadFileService
 * @Description: 这个类主要用于下载对账文件到本地
 * @author Hippo
 * @date 2014-9-14
 * @version 1.0
 */
public class DownLoadFileService{
	private static final Logger logger = Logger.getLogger(DownLoadFileService.class);

	/**
	 * @Title: doDownLoadFile
	 * @Description: 用于下载对账文件到本地
	 * @param localFileName
	 *            本地保存地址
	 * @param remoteFileName
	 *            远程下载地址
	 * @return boolean 返回类型 下载成功返回 true 失败false
	 * @throws
	 */
	public boolean doDownLoadFile(String localFileName, String remoteFileName) {
		logger.info("[文件下载Service]开始从远程地址下载对账文件,remoteFileName=" + remoteFileName + ",localFileName=" + localFileName);
		if (localFileName == null || remoteFileName == null) {
			logger.error("[文件下载Service]参数错误");
			return false;
		}
		File file = new File(localFileName);
		File dir = new File(localFileName.substring(0, localFileName.lastIndexOf("/") + 1));
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		HttpURLConnection httpCon = null;
		BufferedWriter bw = null;
		BufferedReader in = null;
		InputStream content = null;
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdir();
		}
		try {
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			bw = new BufferedWriter(fw);
			URL resourceUrl = new URL(remoteFileName);
			// //获取CP远程连接
			httpCon = (HttpURLConnection) resourceUrl.openConnection();
			httpCon.setConnectTimeout(60000);
			httpCon.setReadTimeout(60000);
			httpCon.setDoInput(true);
			httpCon.setDoOutput(false);
			// /获取远程流信息
			content = (InputStream) httpCon.getContent();
			in = new BufferedReader(new InputStreamReader(content, "UTF-8"));
			String lines = null;
			// //读取头文件
			String header = in.readLine();
			if (header == null) {
				logger.error("[文件下载Service]文件无头部信息,错误文件路径:" + remoteFileName);
				return false;
			}
			// 保存头信息
			bw.write(header);
			bw.write("\r\n");
			// 获取行信息并保存
			while ((lines = in.readLine()) != null) {
				bw.write(lines + "\r\n");
				lines = null;
			}
			// 清理缓存并关闭流
			bw.flush();
		} catch (IOException e) {
			logger.error("[文件下载Service]错误文件路径:remoteUrl=" + remoteFileName, e);
			return false;
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
				if (in != null) {
					in.close();
				}
				if (content != null) {
					content.close();
				}
				if (httpCon != null) {
					httpCon.disconnect();
				}
			} catch (IOException e) {
				logger.error("[文件下载Service]关闭资源失败", e);
			}

		}
		logger.info("[文件下载Service]从远程地址下载对账文件完成,remoteFileName=" + remoteFileName + ",localFileName=" + localFileName);
		return true;
	}

}
