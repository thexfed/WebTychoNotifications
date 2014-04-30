package com.federowicz.webtycho;

/**
 * Holds information related to a study group post.
 * 
 * @author cf
 */
class Post {
	/** Title number of the post **/
	private String titleNum;
	/** Title **/
	private String title;
	/** Author **/
	private String author;
	/** Link to post details **/
	private String link;
	
	/** Date posted **/
	private String date;
	/** Message contents **/
	private String message;

	/**
	 * @return the titleNum
	 */
	public String getTitleNum() {
		return titleNum;
	}
	/**
	 * @param titleNum the titleNum to set
	 */
	public void setTitleNum(String titleNum) {
		this.titleNum = titleNum;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}
	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}
	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(String date) {
		this.date = date;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
}