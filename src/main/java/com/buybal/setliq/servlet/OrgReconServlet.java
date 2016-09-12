package com.buybal.setliq.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buybal.epay.model.Tdatadic;
import com.buybal.epay.service.common.DataDicService;
import com.buybal.setliq.runnable.OrgReconLiqRunnable;
import com.buybal.util.StringUtils;


/**
 * 机构对账入口
 * @date 2014-9-14
 * @author hippo
 */
@WebServlet("/OrgReconServlet")
public class OrgReconServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(OrgReconServlet.class);
	private static final long serialVersionUID = 1L;
       
    public OrgReconServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setCharacterEncoding("UTF-8");
			String remoteFileName = request.getParameter("downLoadUrl");// 远程文件下载路径
			// 判断远程文件地址是否存在
			if (StringUtils.isEmpty(remoteFileName)) {
				logger.error("[对账]无下载文件地址信息");
				return;
			}
			String bankId = request.getParameter("privData");// 私有数据
			// 银行编号
			if (StringUtils.isEmpty(bankId)) {
				logger.error("[对账]无下载文件地址信息");
				return;
			}

			// 检查文件名字格式，获得文件清算日期机构号
			String fileName = remoteFileName.substring(remoteFileName.lastIndexOf("/") + 1);
			String sl[] = fileName.split("_");
			if (sl.length != 3) {
				logger.error("文件名格式错误");
				return;
			}

			String orgCode = sl[0];// 平台机构号
			String liqDate = sl[1];// 格式为yyyyMMdd
			DataDicService dds = new DataDicService();
			Tdatadic Tdatadic = dds.getDataDic("FILE_URL", "sharefile");
			if (Tdatadic == null) {
				logger.error("字典表文件存放路径参数不存在FILE_URL+sharefile");
				return;
			}
			String localFileName = Tdatadic.getDIC_NAME() + "bankRecon/" + orgCode + "/"+fileName; // 文件本地保存地址
			logger.info("[对账开始]保存地址=" + localFileName + ",下载地址=" + remoteFileName);
			
			// 返回成功提示消息，必须要带有"OK"字样
			StringBuffer sb = new StringBuffer();
			sb.append("<html><head></head><body>").append("\r\n");
			sb.append("OK").append("\r\n");
			sb.append("</body></html>").append("\r\n");
			response.setContentLength(sb.toString().length());
			PrintWriter out = response.getWriter();
			out.print(sb.toString());
			out.flush();
			response.flushBuffer();

			// 启动处理线程
			Runnable run = new OrgReconLiqRunnable(orgCode, remoteFileName, localFileName, liqDate,bankId);
			run.run();
		} catch (Exception e) {
			logger.error("[机构对账]对账在主流程Servlet内出错", e);
		}
	}

}
