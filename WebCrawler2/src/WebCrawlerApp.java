import java.io.FileNotFoundException;

public class WebCrawlerApp {
	public static void main(String[] args) throws FileNotFoundException {

		if (args.length < 2) {
			System.out.println("invalid input arguments");
			System.out.println("./webcrawler [username] [password]");
			System.exit(1);
		}

		String username = args[0];
		String passwd = args[1];
		WebCrawler crawler = new WebCrawler(username, passwd);

		crawler.init();
		crawler.process();
	}
}
