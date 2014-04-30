package com.federowicz.webtycho;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;

/**
 * Email utilities.
 * 
 * @author cf
 */
public final class EmailUtilities {

	/**
	 * Send a new email
	 * 
	 * @param config configuration 
	 * @param content message
	 * @param recipients recipients
	 * @param subject email subject
	 */
	public static void sendEmail(final Properties config, String content, List<String> recipients, String subject) {
		try {
			boolean debug = true;
			
			// create some properties and get the default Session
			Properties props = new Properties();
			props.put("mail.smtp.connectiontimeout", 20000);
			props.put("mail.smtp.timeout", 20000);
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.auth", "true");
//			props.put("mail.smtp.port", "587");
//			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.transport.protocol", "smtp");
			
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

			Session session = null;
			session = Session.getInstance(props, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(config.getProperty("email.gmail.user"), config.getProperty("email.gmail.password"));
				}
			});
			session.setDebug(debug);
			
			// create a message
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(config.getProperty("email.from.address"), config.getProperty("email.from.name"), "UTF-8"));
			for (String recipient : recipients) {
				if (StringUtils.isNotBlank(recipient)) {
					msg.addRecipients(javax.mail.Message.RecipientType.TO, recipient);
				}
			}

			msg.addHeader("Reply-To", config.getProperty("email.from.name") + " <" + config.getProperty("email.from.address") + ">");
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			
			msg.setContent(content, "text/html; charset=\"UTF-8\"");
			
			Transport.send(msg);
		} catch (Exception mex) {
			throw new RuntimeException("Could not send e-mail", mex);
		}
	}
}
