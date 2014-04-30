package com.federowicz.webtycho;

import org.apache.http.HttpResponse;

public class WebResponse {
	private HttpResponse response;
	String responseStr;
	
	public WebResponse(HttpResponse response, String responseStr) {
		super();
		this.response = response;
		this.responseStr = responseStr;
	}
	
	
}