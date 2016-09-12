import com.buybal.setliq.service.UmpayReconciliationService;


public class UmpayCheckFileTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		UmpayReconciliationService service=new UmpayReconciliationService();
		service.downLoadFile();
		service.loadFile("UMPAY");
		
//		String s="6226,,13800000000,0000000000000104,20140328,20140328,1,01,2,20140328,01,1,0,P1510000,,140836";
//		System.out.println("20140301".substring(0,8));
	}

}
