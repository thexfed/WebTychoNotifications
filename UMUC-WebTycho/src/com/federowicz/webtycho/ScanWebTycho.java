package com.federowicz.webtycho;

import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

/**
 * Log into WebTycho and look for new Grades and Study Group posts.
 * 
 * @author cf
 */
public class ScanWebTycho {

	private static final Log log = LogFactory.getLog(ScanWebTycho.class);
	
	private Properties config;
	private HttpClientContext context;
	private CloseableHttpClient httpclient;
	private RequestConfig localConfig;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		new ScanWebTycho().runFullProcess();
	}
	
	/**
	 * Constructor - loads config.
	 * 
	 * @throws IOException
	 */
	public ScanWebTycho() throws IOException {
		// Load up config
		config = new Properties();
		String configStr = FileUtils.readFileToString(new File("config.properties"));
		config.load(new StringReader(configStr));
	}

	/**
	 * Executes the full process.
	 * 
	 * @throws Exception
	 */
	private void runFullProcess() throws Exception {
		log.debug("runFullProcess()");
		
		// Load up posts
		List<Post> posts = FileUtilities.loadPosts();
		Map<String, Post> postLinkMap = new HashMap<String, Post>();
		for (Post post : posts) {
			postLinkMap.put(post.getLink(), post);
		}
		log.debug("Loaded " + posts.size() + ", map has " + postLinkMap.size() + " entries");
		

		// Set up http client
		CookieStore cookieStore = new BasicCookieStore();
		context = HttpClientContext.create();
		context.setCookieStore(cookieStore);

		RequestConfig globalConfig = RequestConfig.custom()
		        .setCookieSpec(CookieSpecs.STANDARD)
		        .build();
		httpclient = HttpClients.custom()
		        .setDefaultRequestConfig(globalConfig)
		        .setDefaultCookieStore(cookieStore)
		        .build();
		localConfig = RequestConfig.copy(globalConfig)
		        .setCookieSpec(CookieSpecs.STANDARD)
		        .build();
		
		// Request WebTycho pages in the order that gets down to the class information

		log.debug("##################### Request 1 ############################");
		HttpGet httpGet = new HttpGet("http://tychousa.umuc.edu");
		httpGet.setConfig(localConfig);
		CloseableHttpResponse response = httpclient.execute(httpGet, context);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response.getStatusLine());
		}
		String responseStr = IOUtils.toString(response.getEntity().getContent());
		
		// Cookie origin details
		CookieOrigin cookieOrigin = context.getCookieOrigin();
		// Cookie spec used
		CookieSpec cookieSpec = context.getCookieSpec();
		String host = context.getTargetHost().getHostName();
		
		
		log.debug("##################### Request 2 ############################");
		HttpGet httpGet2 = new HttpGet("http://" + host + "/WebTycho.nsf");
		httpGet2.setConfig(localConfig);
		CloseableHttpResponse response2 = httpclient.execute(httpGet2, context);
		if (response2.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response2.getStatusLine());
		}
		String responseStr2 = IOUtils.toString(response2.getEntity().getContent());

		log.debug("##################### Request 3 ############################");
		HttpGet httpGet3 = new HttpGet("http://" + host + "/sys/login.html?/WebTycho.nsf/");
		BasicClientCookie cookie = new BasicClientCookie("WM_acceptsCookies", "yes");
		cookie.setDomain(".umuc.edu");
		cookieStore.addCookie(cookie);
		httpGet3.setConfig(localConfig);
		CloseableHttpResponse response3 = httpclient.execute(httpGet3, context);
		if (response3.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response3.getStatusLine());
		}
		String responseStr3 = IOUtils.toString(response3.getEntity().getContent());

		log.debug("##################### Request 4 - Login ############################");
		HttpPost httpPost1 = new HttpPost("https://tychong.umuc.edu/tycho/system/login.tycho");
		cookie = new BasicClientCookie("DS9SCHOOL", "umuc");
		cookie.setDomain(".umuc.edu");
		cookieStore.addCookie(cookie);
		httpPost1.setConfig(localConfig);
		httpPost1.addHeader("Origin", "https://" + host);
		httpPost1.addHeader("Referer", "https://" + host + "/sys/login.html?/WebTycho.nsf");
		
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("username", config.getProperty("webtycho.username")));
		params.add(new BasicNameValuePair("password", config.getProperty("webtycho.password")));
		params.add(new BasicNameValuePair("RedirectTo", "http://" + host + "/WebTycho.nsf"));
		UrlEncodedFormEntity e = new UrlEncodedFormEntity(params);
		httpPost1.setEntity(e);
		CloseableHttpResponse response4 = httpclient.execute(httpPost1, context);
		if (response4.getStatusLine().getStatusCode() < 200 || response4.getStatusLine().getStatusCode() >= 400) {
			throw new RuntimeException("Status was not 320: " + response4.getStatusLine());
		}
		String responseStr4 = IOUtils.toString(response4.getEntity().getContent());

		Header[] headers = response4.getAllHeaders();
		Pattern cookiePattern = Pattern.compile("(\\w+)=([^;]*); Path=([^;]*); Domain=(\\S*).*?");
		for (int i = 0; i < headers.length; i++) {
			log.debug("Header: " + response4.getAllHeaders()[i].getName() + " -> " + response4.getAllHeaders()[i].getValue());
			if ("Set-Cookie".equals(headers[i].getName())) {
				Matcher m = cookiePattern.matcher(headers[i].getValue());
				if (m.matches()) {
					String name = m.group(1);
					String value = m.group(2);
					String path = m.group(3);
					String domain = m.group(4);
					log.debug("Name=" + name + ", Value=" + value + ", Path=" + path + ", Domain=" + domain);
					cookie = new BasicClientCookie(name, value);
					cookie.setDomain(domain);
					cookie.setPath(path);
					cookieStore.addCookie(cookie);
				} else {
					log.debug("NO MATCH");
				}
			}
			if ("JSESSIONID".equals(headers[i].getName())) {
				
			} else if ("LtpaToken".equals(headers[i].getName())) {
				
			}
		}
		
		// Test being logged in
		log.debug("####################### Request 5 ##########################");
		HttpGet httpGet5 = new HttpGet("http://tychousa.umuc.edu/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf/Menu?OpenFrameSet&Login");
		httpGet5.setConfig(localConfig);
		httpGet5.addHeader("Referer", "http://" + host + "/WebTycho.nsf/ClassList?OpenForm&" + config.getProperty("webtycho.username") + "");
		CloseableHttpResponse response5 = httpclient.execute(httpGet5, context);
		if (response5.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response5.getStatusLine());
		}
		String responseStr5 = IOUtils.toString(response5.getEntity().getContent());
		log.debug("#################################################");

		log.debug("####################### Request 6 ##########################");
		HttpGet httpGet6 = new HttpGet("http://tychousa.umuc.edu/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf/" + config.get("studyGroup.id") + "+by+Topic?OpenView&ExpandView");
		httpGet6.setConfig(localConfig);
		CloseableHttpResponse response6 = httpclient.execute(httpGet6, context);
		if (response6.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response6.getStatusLine());
		}
		String responseStr6 = IOUtils.toString(response6.getEntity().getContent());
		log.debug("#################################################");
		
		
		processPosts(responseStr6, posts, postLinkMap);

		
		//requestURL(null, "/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf/Menu?OpenFrameSet&Login", "headers.txt", "response2.txt");

		// ############################ Check Grades ####################################
		
		log.debug("####################### Request 7 ##########################");
		HttpGet httpGet7 = new HttpGet("http://tychousa.umuc.edu/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf/PortfolioNG?OpenAgent&Student=" + config.getProperty("webtycho.username") + "&PKey=NG3P0rtf0li0&cgServer=tychousa.umuc.edu");
		httpGet7.setConfig(localConfig);
		CloseableHttpResponse response7 = httpclient.execute(httpGet7, context);
		if (response7.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response7.getStatusLine());
		}
		String responseStr7 = IOUtils.toString(response7.getEntity().getContent());
		checkGrades(responseStr7);
		log.debug("#################################################");
	}
	
	/**
	 * Check for new grades
	 * 
	 * @param gradePageStr grade page content
	 */
	private void checkGrades(String gradePageStr) throws Exception {
		// Read in existing grades
		List<Grade> grades = FileUtilities.loadGrades();
		Map<String, Grade> gradeMap = new HashMap<String, Grade>();
		for (Grade grade : grades) {
			gradeMap.put(grade.getTitle(), grade);
		}

		List<Grade> newGrades = new ArrayList<Grade>();
		
		// Check 1st section of grades
		String leftover = "";
		Pattern assnSubPattern = Pattern.compile("(?m)(?s).*?<tr>\\s*<td>(.*?)</td>\\s*<td>([^<]*?)</td>\\s*<td>(Graded)</td>\\s*<td>([^<]*?)</td>\\s*<td>([^<]*?)</td>\\s*</tr>(.*)");
		Matcher m = assnSubPattern.matcher(gradePageStr);
		while (m.matches()) {
			String title = StringUtils.trim(m.group(1).replaceAll("<[^>]+>", ""));
			String date = m.group(2);
			String status = m.group(3);
			String gradeStr = m.group(4);
			String comments = m.group(5).replaceAll("\\s+", " ");
			log.debug("Title: " + title);
			log.debug("Date: " + date);
			log.debug("Status: " + status);
			log.debug("Grade: " + gradeStr);
			log.debug("Comments: " + comments);
			leftover = m.group(6);
			m = assnSubPattern.matcher(leftover);
			
			Grade grade = new Grade(title, gradeStr, comments);
			if (!gradeMap.containsKey(title)) {
				log.debug("## NEW GRADE ##");
				newGrades.add(grade);
				grades.add(grade);
			}
			
			log.debug("");
		}

		// Check 2nd section of grades
		Pattern otherWorkPattern = Pattern.compile("(?m)(?s).*?<tr>\\s*<td>([^<]*?)</td>\\s*<td>(Graded)</td>\\s*<td>([^<]*?)</td>\\s*<td>([^<]*?)</td>\\s*</tr>(.*)");
		m = otherWorkPattern.matcher(leftover);
		while (m.matches()) {
			String title = m.group(1).trim();
			String status = m.group(2);
			String gradeStr = m.group(3);
			String comments = m.group(4).replaceAll("\\s+", " ");
			log.debug("Title: " + title);
			//log.debug("Date: " + date);
			log.debug("Status: " + status);
			log.debug("Grade: " + gradeStr);
			log.debug("Comments: " + comments);
			leftover = m.group(5);
			m = otherWorkPattern.matcher(leftover);

			Grade grade = new Grade(title, gradeStr, comments);
			if (!gradeMap.containsKey(title)) {
				log.debug("## NEW GRADE ##");
				newGrades.add(grade);
				grades.add(grade);
			}

			log.debug("\n");
		}
		
		// Save grades
		FileUtilities.saveGrades(grades);
		
		// If there are any new ones, send an email
		if (newGrades.size() > 0) {
			StringBuffer content = new StringBuffer();
			content.append("You have a new grade in " + config.getProperty("class.number") + ":<br/>");
			content.append("<br/>");
			content.append("<table border='1' cellpadding='2' cellspacing='0'><tr><td>Title</td><td>Grade</td><td>Comments</td></tr>\n");
			for (Grade grade : newGrades) {
				content.append("<tr>");
				content.append("<td valign='top' align='center'>" + grade.getTitle() + "</td>");
				content.append("<td valign='top' align='center'><b>" + grade.getGrade() + "</b></td>");
				content.append("<td valign='top'>" + grade.getComments() + "</td>");
				content.append("</tr>");
			}
			content.append("</table>");
			EmailUtilities.sendEmail(config, content.toString(), Arrays.asList(config.getProperty("email.grade.recipient")), "New " + config.getProperty("class.number") + " Grade");
		}
	}

	/**
	 * Reads the post page contents and looks for new posts.
	 * 
	 * @param responseStr page content of group posts
	 * @param posts existing list of Posts, to which new ones will be added
	 * @param postLinkMap Map of post links to the associated Post 
	 * @throws Exception
	 */
	private void processPosts(String responseStr, List<Post> posts, Map<String, Post> postLinkMap) throws Exception {
		List<Post> newPosts = new ArrayList<Post>();

		Pattern responseLinePattern = Pattern.compile("<tr valign=\"top\".*</tr>");
		
		Pattern lineLinkPattern = Pattern.compile(".*<a href=\"(/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf/[\\w/]+\\?OpenDocument)\">.*");
		Pattern lineTitlePattern1 = Pattern.compile(".*width='11' height='9' border=0>\\s*([\\d\\.]+)\\s*<B>([^<]+)</B>.*");
		Pattern lineTitlePattern2 = Pattern.compile(".*width='11' height='9' border=0> <FONT SIZE=\\-1>([\\d\\.]+)\\s*([^<]+)<.*");
		Pattern lineAuthorPattern1 = Pattern.compile(".*?<font size=\"2\" face=\"Arial\">([^<]+)</font>.*");
		Pattern lineAuthorPattern2 = Pattern.compile(".*<font color=#000000><i>\\(([^,]+),  \\d+/\\d+/\\d+ \\d+\\:\\d+\\)</i>.*");
		
		String[] responseLines = StringUtils.split(responseStr, "\n");
		for (int i = 0; i < responseLines.length; i++) {
			Matcher m = responseLinePattern.matcher(responseLines[i]);
			if (m.matches()) {
				if (responseLines[i].indexOf("/" + config.getProperty("class.number") + "/" + config.getProperty("class.semester") + "/" + config.getProperty("class.section") + "/class.nsf") >= 0) {
					log.debug("Found a post: " + responseLines[i]);

					Matcher linkMatcher = lineLinkPattern.matcher(responseLines[i]);
					Matcher titleMatcher1 = lineTitlePattern1.matcher(responseLines[i]);
					Matcher titleMatcher2 = lineTitlePattern2.matcher(responseLines[i]);
					Matcher authorMatcher1 = lineAuthorPattern1.matcher(responseLines[i]);
					Matcher authorMatcher2 = lineAuthorPattern2.matcher(responseLines[i]);
					
					String link = "";
					String titleNum = "";
					String title = "";
					String author = "";
					if (linkMatcher.matches()) {
						link = linkMatcher.group(1);
					}
					if (titleMatcher1.matches()) {
						titleNum = titleMatcher1.group(1);
						title = titleMatcher1.group(2);
					} else if (titleMatcher2.matches()) {
						titleNum = titleMatcher2.group(1);
						title = titleMatcher2.group(2);
					}
					if (authorMatcher1.matches()) {
						author = authorMatcher1.group(1);
					} else if (authorMatcher2.matches()) {
						author = authorMatcher2.group(1);
					}
					
					Post post = new Post();
					post.setTitleNum(titleNum);
					post.setTitle(title);
					post.setLink(link);
					post.setAuthor(author);
					
					log.debug("\tTitle: " + titleNum + " -> " + title);
					log.debug("\tAuthor: " + author);
					log.debug("\tLink: " + link);
					
					if (postLinkMap.containsKey(post.getLink())) {
						log.debug("\tAlready exists in data");
					} else {
						log.debug("\tNEW ENTRY!!!");
						newPosts.add(post);
						posts.add(post);
					}
					
				}
			}
		}

		if (newPosts.size() > 0) {
			// send email alert
			sendStudyGroupPostEmail(newPosts);
		}
		FileUtilities.savePosts(posts);
	}

	/**
	 * Send an email about a new study group post.
	 * 
	 * @param newPosts new posts
	 * @throws Exception
	 */
	private void sendStudyGroupPostEmail(List<Post> newPosts) throws IOException {
		StringBuffer content = new StringBuffer();
		content.append("There " + (newPosts.size() == 1 ? "was a new post " : "were " +  newPosts.size() + " new posts ") + " to the study group in " + config.getProperty("class.number") + ":<br/>");
		content.append("<br/>");
		for (Post post : newPosts) {
			content.append("<b>" + post.getAuthor() + "</b><br/>");

			retrievePostMessageContents(post);
			
			content.append(post.getTitle() + " (" + post.getDate() + ")<br/>");
			content.append("<br/>");
			content.append(post.getMessage() + "<br/>");
			content.append("<br/>");
			content.append("<hr/>");
			content.append("<br/>");
		}
		
		List<String> recipients = Arrays.asList(StringUtils.split(config.getProperty("email.studyGroup.recipients"), ","));
		EmailUtilities.sendEmail(config, content.toString(), recipients, config.getProperty("class.number") + " Study Group Post");
	}

	/**
	 * Retrieve the contents of a study group post
	 * 
	 * @param Post the post
	 * @return
	 */
	private void retrievePostMessageContents(Post post) throws IOException {
		log.debug("retrievePostMessageContents( " + post + ")");
		HttpGet httpGet = new HttpGet("http://tychousa.umuc.edu" + post.getLink());
		httpGet.setConfig(localConfig);
		CloseableHttpResponse response = httpclient.execute(httpGet, context);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Status was not 200: " + response.getStatusLine());
		}
		String responseStr = IOUtils.toString(response.getEntity().getContent());
		log.debug("#################################################");

		Pattern datePattern = Pattern.compile("(?sm).*>Created<\\/font>: <font size=\"2\" face=\"Arial\">([^<]+)<\\/font>.*");
		Pattern messagePattern = Pattern.compile("(?sm).*<\\/style><br>\\s*(.*?)<Script>\\s*var\\s*attDB.*");
		
		Matcher dateMatcher = datePattern.matcher(responseStr);
		if (dateMatcher.matches()) {
			log.debug("Date: " + dateMatcher.group(1));
			post.setDate(dateMatcher.group(1));
		} else {
			log.debug("NO DATE MATCH");
		}
		;
		Matcher msgMatcher = messagePattern.matcher(responseStr);
		if (msgMatcher.matches()) {
			log.debug("Message: " + msgMatcher.group(1));
			post.setMessage(msgMatcher.group(1));
		} else {
			log.debug("NO MESSAGE MATCH");
		}
	}
	
}
