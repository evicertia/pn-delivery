package it.pagopa.pn.delivery.rest;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.delivery.svc.sentnotification.NotificationSentService;

@WebFluxTest(PnSentNotificationController.class)
class PnSentNotificationControllerTest {
	
	private static final String IUN = "IUN";
	private static final String USER_ID = "USER_ID";
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationSentService svc;

	@Test
	void getSentNotificationSuccess() {
		// Given		
		Notification notification = newNotification();
		
		// When
		Mockito.when( svc.getSentNotification( Mockito.anyString() ) ).thenReturn( notification );
				
		// Then		
		webTestClient.get()
			.uri( "/delivery/notifications/received/" + IUN  )
			.accept( MediaType.ALL )
			.header(HttpHeaders.ACCEPT, "application/json")
			.header( "X-PagoPA-PN-PA", USER_ID )
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Notification.class);
		
		Mockito.verify( svc ).getSentNotification(IUN);
	}

	private Notification newNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc00")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc01")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
    }
	
}
