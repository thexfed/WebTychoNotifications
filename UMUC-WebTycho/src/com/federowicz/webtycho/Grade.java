package com.federowicz.webtycho;

/**
 * Holds information related to a class grade.
 * 
 * @author cf
 */
public class Grade {
	/** Title of assignment. */
	private final String title;
	/** Grade received */
	private final String grade;
	/** Instructor comments */
	private final String comments;
	
	/**
	 * @param title
	 * @param grade
	 * @param comments
	 */
	public Grade(String title, String grade, String comments) {
		this.title = title;
		this.grade = grade;
		this.comments = comments;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the grade
	 */
	public String getGrade() {
		return grade;
	}

	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}
	
}