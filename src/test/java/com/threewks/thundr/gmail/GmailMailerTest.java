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


import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.threewks.thundr.request.RequestContainer;
import com.threewks.thundr.view.ViewResolverRegistry;
import com.threewks.thundr.view.string.StringView;
import com.threewks.thundr.view.string.StringViewResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GmailMailerTest {

	@Mock private GoogleAuthorizationCodeFlow flow;
	@Mock private Gmail gmail;
	@Mock private Gmail.Users users;
	@Mock private Gmail.Users.Messages messages;
	@Mock private Gmail.Users.Messages.Send send;
	@Mock private RequestContainer requestContainer;

	@Captor private ArgumentCaptor<Message> messageCaptor;

	private GmailMailer mailer;

	@Before
	public void before() throws IOException {

		ViewResolverRegistry viewResolverRegistry = new ViewResolverRegistry();
		viewResolverRegistry.addResolver(StringView.class, new StringViewResolver());

		mailer = spy(new GmailMailer(viewResolverRegistry, flow, requestContainer));

		doReturn(gmail).when(mailer).getClient();
		when(gmail.users()).thenReturn(users);
		when(users.messages()).thenReturn(messages);
		when(messages.send(eq("me"), messageCaptor.capture())).thenReturn(send);
	}

	@Test
	public void shouldSend() throws IOException, MessagingException {

		Map<String, String> to = new LinkedHashMap<>();
		to.put("recipient1@email.com", "Recipient 1");
		to.put("recipient2@email.com", "Recipient 2");

		// @formatter:off
		mailer.mail()
				.from("sender@email.com")
				.to(to)
				.subject("Test subject")
				.bcc("bcc@email.com")
				.cc("cc@email.com")
				.replyTo("reply@email.com")
				.body(new StringView("This is a test message"))
				.send();
		// @formatter:on

		verify(send).execute();
		Message message = messageCaptor.getValue();

		String messageText = new String(Base64.decodeBase64(message.getRaw()));
		System.out.println(messageText);

		// this is nasty, but the Gmail API is very awkward to test so this is the best we can do
		assertThat(messageText, containsString("From: sender@email.com"));
		assertThat(messageText, containsString("Reply-To: reply@email.com"));
		assertThat(messageText, containsString("To: Recipient 1 <recipient1@email.com>, Recipient 2 <recipient2@email.com>"));
		assertThat(messageText, containsString("Cc: cc@email.com"));
		assertThat(messageText, containsString("Bcc: bcc@email.com"));
		assertThat(messageText, containsString("Subject: Test subject"));
		assertThat(messageText, containsString("This is a test message"));
	}

}
