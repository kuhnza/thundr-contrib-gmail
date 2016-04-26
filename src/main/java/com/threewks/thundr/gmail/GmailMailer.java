/*
 * This file is a component of thundr, a software library from 3wks.
 * Read more: http://3wks.github.io/thundr/
 * Copyright (C) 2015 3wks, <thundr@3wks.com.au>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threewks.thundr.gmail;

import com.atomicleopard.expressive.ETransformer;
import com.atomicleopard.expressive.Expressive;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.threewks.thundr.exception.BaseException;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.mail.Attachment;
import com.threewks.thundr.mail.BaseMailer;
import com.threewks.thundr.mail.Mailer;
import com.threewks.thundr.view.BasicViewRenderer;
import com.threewks.thundr.view.ViewResolverRegistry;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class GmailMailer extends BaseMailer implements Mailer {

    public static final String CREDENTIAL_USER_ID = "gmail-credentials";

    private final GoogleAuthorizationCodeFlow flow;

    private Gmail gmail;

    public GmailMailer(ViewResolverRegistry viewResolverRegistry, GoogleAuthorizationCodeFlow gmailAuthorizationCodeFlow) {
        super(viewResolverRegistry);
        this.flow = gmailAuthorizationCodeFlow;
    }

    Gmail getClient() {
        if (gmail == null) {
            initClient();
        }
        return gmail;
    }

    protected void initClient() {
        Credential credential;
        try {
            credential = flow.loadCredential(CREDENTIAL_USER_ID);
        } catch (IOException e) {
            String message = String.format("Error loading stored credential for user %s: %s", CREDENTIAL_USER_ID, e.getMessage());
            Logger.error(message);
            throw new BaseException(e, message);
        }
        gmail = new Gmail.Builder(flow.getTransport(), flow.getJsonFactory(), credential).build();
    }

    @Override
    protected void sendInternal(Map.Entry<String, String> from, Map.Entry<String, String> replyTo, Map<String, String> to, Map<String, String> cc, Map<String, String> bcc, String subject, Object body, List<Attachment> attachments) {
        sendGmailInternal(from, replyTo, to, cc, bcc, subject, body, attachments);
    }

    protected void sendGmailInternal(Map.Entry<String, String> from, Map.Entry<String, String> replyTo, Map<String, String> to, Map<String, String> cc, Map<String, String> bcc, String subject, Object body, List<Attachment> attachments) {
        String content = render(body).getOutputAsString();

        InternetAddress fromAddress = Transformers.FormatInternetAddress.from(from);
        InternetAddress replyToAddress = null;

        Set<InternetAddress> toAddresses = null;
        Set<InternetAddress> ccAddresses = null;
        Set<InternetAddress> bccAddresses = null;

        if (Expressive.isNotEmpty(to)) {
            toAddresses = getInternetAddresses(to);
        }
        if (Expressive.isNotEmpty(cc)) {
            ccAddresses = getInternetAddresses(cc);
        }
        if (Expressive.isNotEmpty(bcc)) {
            bccAddresses = getInternetAddresses(bcc);
        }
        if (replyTo != null) {
            replyToAddress = Transformers.FormatInternetAddress.from(replyTo);
        }

        MimeMessage mimeMessage = createEmailWithAttachment(toAddresses, fromAddress, ccAddresses, bccAddresses,
                replyToAddress, subject, content, attachments);

        Message message = createMessageWithEmail(mimeMessage);
        try {
            getClient().users().messages().send("me", message).execute();
        } catch (IOException e) {
            Logger.error("Failed to send email: %s", e.getMessage());
            throw new GmailException(e);
        }
    }

    private Set<InternetAddress> getInternetAddresses(Map<String, String> addressStrings) {
        Set<InternetAddress> addresses = new LinkedHashSet<>();
        for (Map.Entry<String, String> toAddrStr : addressStrings.entrySet()) {
            addresses.add(Transformers.FormatInternetAddress.from(toAddrStr));
        }
        return addresses;
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to       Email address of the receiver.
     * @param from     Email address of the sender, the mailbox account.
     * @param subject  Subject of the email.
     * @param bodyText Body text of the email.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    protected MimeMessage createEmailWithAttachment(Set<InternetAddress> to, InternetAddress from, Set<InternetAddress> cc,
                                                    Set<InternetAddress> bcc, InternetAddress replyTo, String subject,
                                                    String bodyText, List<Attachment> attachments) {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        try {

            email.setFrom(from);

            if (to != null) {
                email.addRecipients(javax.mail.Message.RecipientType.TO, to.toArray(new InternetAddress[to.size()]));
            }
            if (cc != null) {
                email.addRecipients(javax.mail.Message.RecipientType.CC, cc.toArray(new InternetAddress[cc.size()]));
            }
            if (bcc != null) {
                email.addRecipients(javax.mail.Message.RecipientType.BCC, bcc.toArray(new InternetAddress[bcc.size()]));
            }
            if (replyTo != null) {
                email.setReplyTo(new Address[]{replyTo});
            }

            email.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(bodyText, "text/html");
            mimeBodyPart.setHeader("Content-Type", "text/html; charset=\"UTF-8\"");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            if (attachments != null) {
                for (Attachment attachment : attachments) {
                    mimeBodyPart = new MimeBodyPart();

                    BasicViewRenderer renderer = new BasicViewRenderer(viewResolverRegistry);
                    renderer.render(attachment.view());
                    byte[] data = renderer.getOutputAsBytes();
                    String attachmentContentType = renderer.getContentType();
                    String attachmentCharacterEncoding = renderer.getCharacterEncoding();

					populateMimeBodyPart(mimeBodyPart, attachment, data, attachmentContentType, attachmentCharacterEncoding);

                    multipart.addBodyPart(mimeBodyPart);
                }
            }

            email.setContent(multipart);
        } catch (MessagingException e) {
            Logger.error(e.getMessage());
            Logger.error("Failed to create email from: %s;%s, to: %s, cc: %s, bcc: %s, replyTo %s;%s, subject %s, body %s, number of attachments %d", from.getAddress(), from.getPersonal(),
					Transformers.InternetAddressesToString.from(to), Transformers.InternetAddressesToString.from(cc), Transformers.InternetAddressesToString.from(bcc),
					replyTo == null ? "null" : replyTo.getAddress(), replyTo == null ? "null" : replyTo.getPersonal(), subject, bodyText, attachments == null ? 0 : attachments.size());
            throw new GmailException(e);
        }

        return email;
    }

	protected void populateMimeBodyPart(MimeBodyPart mimeBodyPart, Attachment attachment, byte[] data, String attachmentContentType, String attachmentCharacterEncoding) throws MessagingException {
		String fullContentType = attachmentContentType + "; charset=" + attachmentCharacterEncoding;
		mimeBodyPart.setFileName(attachment.name());
		mimeBodyPart.setContent(data, fullContentType);
		mimeBodyPart.setDisposition(attachment.disposition().toString());
		if (attachment.isInline()) {
            mimeBodyPart.setContentID("<" + attachment.name() + ">");
        }
	}

	/**
     * Create a Message from an email
     *
     * @param email Email to be set to raw of message
     * @return Message containing base64url encoded email.
     * @throws IOException
     * @throws MessagingException
     */
    protected Message createMessageWithEmail(MimeMessage email) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            email.writeTo(bytes);
        } catch (MessagingException | IOException e) {
            Logger.error("Could not write email to output stream: %s", e.getMessage());
            throw new GmailException(e);
        }
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static class Transformers {

        public static final ETransformer<Map.Entry<String, String>, InternetAddress> FormatInternetAddress = new ETransformer<Map.Entry<String, String>, InternetAddress>() {
            @Override
            public InternetAddress from(Map.Entry<String, String> address) {
                try {
                    return new InternetAddress(address.getKey(), address.getValue());
                } catch (UnsupportedEncodingException e) {
                    Logger.error(e.getMessage());
                    Logger.error("Error converting email address %s, %s to an Internet Address", address.getKey(), address.getValue());
                    throw new GmailException(e);
                }
            }
        };
        public static final ETransformer<Set<InternetAddress>, String> InternetAddressesToString = new ETransformer<Set<InternetAddress>, String>() {
            @Override
            public String from(Set<InternetAddress> internetAddresses) {
                StringBuilder builder = new StringBuilder();
                for (InternetAddress address: internetAddresses) {
                    builder.append(address.getAddress())
                    .append(",")
                    .append(address.getPersonal());
                }
                return builder.toString();
            }
        };
    }
}
