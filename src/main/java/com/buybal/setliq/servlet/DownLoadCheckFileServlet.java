package com.buybal.setliq.servlet;

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

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buybal.epay.model.NetPos;
import com.buybal.epay.service.NetPosService;
import com.buybal.setliq.receivemail.service.impl.ReceiveMailServiceImpl;
import com.buybal.setliq.service.ReconciliationService;
import com.buybal.util.PropertiseUtil;

/**
 * Servlet implementation class DownLoadCheckFileServlet
 */
@WebServlet("/DownLoadCheckFileServlet")
public class DownLoadCheckFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(DownLoadCheckFileServlet.class);
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DownLoadCheckFileServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost( request,  response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//下载文件
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		response.setContentType("text/html;charset=utf-8");
		ServletOutputStream outStream = null;
		HttpURLConnection httpCon = null;
		BufferedWriter bw = null;
		BufferedReader in = null;
		InputStream content = null;
		try {
			String downLoadUrl = request.getParameter("downLoadUrl");
			System.out.println("文件下载地址=" + downLoadUrl);
			if(downLoadUrl == null || "".equals(downLoadUrl)){
				return ;
			}
			outStream = response.getOutputStream();
			outStream.write("OK".getBytes("UTF-8"));
			outStream.flush();
			//截取商户号验证商户是否存在
			String fileName = downLoadUrl.substring(downLoadUrl.lastIndexOf("=")+1);
			String merId= fileName.substring(0,15);//商户号
			String mailAttachPath = PropertiseUtil.getString("setLiq","bankReconPath"); // 附件存放目录
			String saveDir = mailAttachPath+fileName;
			File file = new File(saveDir);
			//判断目录是否存在
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			URL resourceUrl = new URL(downLoadUrl);
			// //获取CP远程连接
			httpCon = (HttpURLConnection) resourceUrl.openConnection();
			httpCon.setConnectTimeout(60000);
			httpCon.setReadTimeout(60000);
			httpCon.setDoInput(true);
			httpCon.setDoOutput(true);
			// 获取远程流信息
			content = (InputStream) httpCon.getContent();
			in = new BufferedReader(new InputStreamReader(content, "UTF-8"));
			String lines = null;
			while ((lines = in.readLine()) != null) {
				bw.write(lines + "\r\n");
			}
			// 清理缓存并关闭流
			bw.flush();
			//加载对账文件
			ReconciliationService service = new ReconciliationService();
			service.loadFile(PropertiseUtil.getString("setLiq", "netId_"+merId));
		} catch (Exception e) {
			logger.error("接收对账通知异常",e);
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
				outStream.close();
			} catch (IOException e) {
			}
		}
	}

		
	}

