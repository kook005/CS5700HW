import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;


public class WebCrawlerApp {
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub

		String username = "001102979";
		String passwd = "T2KGV7J2";
//		String username = "001989426";
//		String passwd = "WBBCIW3Y";
		
		
		
		System.setOut(new PrintStream(new File("crawlerLog")));
		WebCrawler crawler = new WebCrawler(username, passwd);

		try {
			crawler.init();
		} catch (Exception e) {
			System.err.println("Something is not correct");
			e.printStackTrace();
			System.exit(1);
		}
		
		crawler.process();
	}
}
