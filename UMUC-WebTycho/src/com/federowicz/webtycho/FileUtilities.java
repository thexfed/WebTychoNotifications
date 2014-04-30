package com.federowicz.webtycho;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Read and write stuff to files.
 * 
 * @author cf
 */
public final class FileUtilities {

	/** Saved grades */
	private static final String GRADE_FILE = "grades.dat";
	/** Saved posts */
	private static final String POST_FILE = "posts.dat";

	/**
	 * Load posts from post file.
	 * 
	 * @return list of Posts
	 * @throws IOException
	 */
	public static List<Post> loadPosts() throws IOException {
		Pattern postFilePattern = Pattern.compile("TitleNum=(.*), Title=(.*), Author=(.*), Link=(.*)");
		
		List lines = FileUtils.readLines(new File(POST_FILE));
		List<Post> posts = new ArrayList<Post>();
		for (Iterator iter = lines.iterator(); iter.hasNext();) {
			String line = (String) iter.next();
			if (StringUtils.isNotBlank(line)) {
				Matcher m = postFilePattern.matcher(line);
				if (m.matches()) {
					Post post = new Post();
					post.setTitleNum(m.group(1));
					post.setTitle(m.group(2));
					post.setAuthor(m.group(3));
					post.setLink(m.group(4));
					posts.add(post);
				} else {
					throw new RuntimeException("Line in post file didn't match pattern: " + line);
				}
			}
		}
		return posts;
	}

	/**
	 * Save posts to a file.
	 * 
	 * @param posts posts to save
	 * @throws IOException
	 */
	public static void savePosts(List<Post> posts) throws IOException {
		List<String> lines = new ArrayList<String>();
		for (Post post : posts) {
			String line = "TitleNum=" + post.getTitleNum() + ", Title=" + post.getTitle() + ", Author=" + post.getAuthor() + ", Link=" + post.getLink();
			lines.add(line);
		}
		FileUtils.writeLines(new File(POST_FILE), lines);
	}
	

	/**
	 * Load grades from grade file.
	 * 
	 * @return list of Grades
	 * @throws IOException
	 */
	public static List<Grade> loadGrades() throws IOException {
		Pattern gradeFilePattern = Pattern.compile("Title\\[\\[(.*)\\]\\], Grade\\[\\[(.*)\\]\\], Comments\\[\\[(.*)\\]\\]");
		
		List lines = FileUtils.readLines(new File(GRADE_FILE));
		List<Grade> grades = new ArrayList<Grade>();
		for (Iterator iter = lines.iterator(); iter.hasNext();) {
			String line = (String) iter.next();
			if (StringUtils.isNotBlank(line)) {
				Matcher m = gradeFilePattern.matcher(line);
				if (m.matches()) {
					Grade grade = new Grade(m.group(1), m.group(2), m.group(3));
					grades.add(grade);
				} else {
					throw new RuntimeException("Line in grade file didn't match pattern: " + line);
				}
			}
		}
		return grades;
	}

	/**
	 * Save grades to a file.
	 * 
	 * @param grades grades to save
	 * @throws IOException
	 */
	public static void saveGrades(List<Grade> grades) throws IOException {
		List<String> lines = new ArrayList<String>();
		for (Grade grade : grades) {
			String line = "Title[[" + grade.getTitle() + "]], Grade[[" + grade.getGrade() + "]], Comments[[" + grade.getComments() + "]]";
			lines.add(line);
		}
		FileUtils.writeLines(new File(GRADE_FILE), lines);
	}
	

}
