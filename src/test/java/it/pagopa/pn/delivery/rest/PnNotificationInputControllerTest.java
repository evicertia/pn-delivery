package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.utils.PreloadRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.Mockito.mock;

@WebFluxTest(PnNotificationInputController.class)
class PnNotificationInputControllerTest {

	private static final String PA_ID = "paId";
	private static final String IUN = "IUN";
	public static final Integer MAX_NUMBER_REQUESTS = 1;
	private static final String SECRET = "secret";
	private static final String URL = "url";
	public static final List<String> GROUPS = List.of("Group1", "Group2");
	private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
	private static final String X_PAGOPA_PN_SRC_CH_DETAILS = "sourceChannelDetails";
	private static final String FILE_SHA_256 = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";

	@Autowired
    WebTestClient webTestClient;

	@SpyBean
	private ModelMapper modelMapper;
	
	@MockBean
	private NotificationReceiverService deliveryService;

	@MockBean
	private NotificationAttachmentService attachmentService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@Test
	void postSuccess() throws PnIdConflictException {
		// Given
		NewNotificationRequestV21 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
						.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
						.paProtocolNumber("protocol_number").build();
				
		// When
		Mockito.when(deliveryService.receiveNotification(
						Mockito.anyString(),
						Mockito.any( NewNotificationRequestV21.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull())
				).thenReturn( savedNotification );

		// Then
		webTestClient.post()
                .uri("/delivery/v2.1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(notificationRequest), NewNotificationRequestV21.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
                .exchange()
                .expectStatus().isAccepted();
		
		Mockito.verify( deliveryService ).receiveNotification(
						PA_ID,
						notificationRequest,
						X_PAGOPA_PN_SRC_CH,
						null,
						GROUPS,
						null);
	}

	@Test
	void postSuccessWithSourceChannelDetails() throws PnIdConflictException {
		// Given
		NewNotificationRequestV21 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber("protocol_number").build();

		// When
		Mockito.when(deliveryService.receiveNotification(
				Mockito.anyString(),
				Mockito.any( NewNotificationRequestV21.class ),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyList(),
				Mockito.isNull())
		).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DETAILS )
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
				PA_ID,
				notificationRequest,
				X_PAGOPA_PN_SRC_CH,
				X_PAGOPA_PN_SRC_CH_DETAILS,
				GROUPS,
				null);
	}

	@Test
	void postSuccessWithNotificationVersion() throws PnIdConflictException {
		// Given
		NewNotificationRequestV21 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber("protocol_number").build();

		// When
		Mockito.when(deliveryService.receiveNotification(
				Mockito.anyString(),
				Mockito.any( NewNotificationRequestV21.class ),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyList(),
				Mockito.anyString())
		).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DETAILS )
				.header(PnDeliveryRestConstants.NOTIFICATION_VERSION_HEADER, "1")
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
				PA_ID,
				notificationRequest,
				X_PAGOPA_PN_SRC_CH,
				X_PAGOPA_PN_SRC_CH_DETAILS,
				GROUPS,
				"1");
	}

	private NewNotificationRequestV21 newNotificationRequest() {
		return NewNotificationRequestV21.builder()
				.group( "group" )
				.senderDenomination( "Comune di Milano" )
				.senderTaxId( "01199250158" )
				.paProtocolNumber( "protocol_number" )
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.recipients( Collections.singletonList( NotificationRecipientV21.builder()
								.recipientType( NotificationRecipientV21.RecipientTypeEnum.PF )
								.taxId( "LVLDAA85T50G702B" )
								.denomination( "Ada Lovelace" )
								.digitalDomicile( NotificationDigitalAddress.builder()
										.type( NotificationDigitalAddress.TypeEnum.PEC )
										.address( "address@domain.it" )
										.build() )
								.physicalAddress( NotificationPhysicalAddress.builder()
										.zip( "83100" )
										.municipality( "municipality" )
										.address( "address" )
										.build() )
								.payments( List.of(NotificationPaymentItem.builder()
										.build() ))
						.build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder()
								.digests( NotificationAttachmentDigests.builder()
										.sha256( FILE_SHA_256 )
										.build() )
								.contentType( "application/pdf" )
								.ref( NotificationAttachmentBodyRef.builder()
										.key( "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG" )
										.versionToken( "version_token" )
										.build() )
						.build() ) )
				.physicalCommunicationType( NewNotificationRequestV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				.subject( "subject_length" )
				.taxonomyCode( "010101P" )
				.build();
	}

	@Test
	void postFailure() {
		// Given
		NewNotificationRequestV21 request = newNotificationRequest();
		Map<String,String> conflictMap = new HashMap<>();
		conflictMap.put( "noticeCode", "duplicatedNoticeCode" );
		PnIdConflictException exception = new PnIdConflictException( conflictMap );

		// When
		Mockito.when( deliveryService.receiveNotification(
						Mockito.anyString(),
						Mockito.any( NewNotificationRequestV21.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.isNull(),
						Mockito.isNull())
				).thenThrow( exception );

		//Then
		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void postFailureBindExc() {
		// Given
		NewNotificationRequestV21 request = newNotificationRequest();
		request.setPaProtocolNumber( null );

		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void postFailureRuntimeExc() {
		// Given
		NewNotificationRequestV21 request = newNotificationRequest();

		Mockito.when( deliveryService.receiveNotification( PA_ID, request, X_PAGOPA_PN_SRC_CH,null, Collections.emptyList(), null ) ).thenThrow( RuntimeException.class );

		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus()
				.is5xxServerError();
	}

	@Test
	void postSuccessWithAmount() throws PnIdConflictException {
		// Given
		NewNotificationRequestV21 notificationRequest = NewNotificationRequestV21.builder()
				.group( "group" )
				.senderDenomination( "Comune di Milano" )
				.senderTaxId( "01199250158" )
				.taxonomyCode("010101P")
				.paProtocolNumber( "protocol_number" )
				.amount(10000)
				.paymentExpirationDate("2023-10-22")
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.recipients( Collections.singletonList( NotificationRecipientV21.builder()
						.recipientType( NotificationRecipientV21.RecipientTypeEnum.PF )
						.taxId( "LVLDAA85T50G702B" )
						.denomination( "Ada Lovelace" )
						.digitalDomicile( NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address( "address@domain.it" )
								.build() )
						.physicalAddress( NotificationPhysicalAddress.builder()
								.zip( "83100" )
								.municipality( "municipality" )
								.address( "address" )
								.build() )
						.payments( List.of(NotificationPaymentItem.builder().build()) )
						.build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder()
						.digests( NotificationAttachmentDigests.builder()
								.sha256( FILE_SHA_256 )
								.build() )
						.contentType( "application/pdf" )
						.ref( NotificationAttachmentBodyRef.builder()
								.key( "PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG" )
								.versionToken( "version_token" )
								.build() )
						.build() ) )
				.physicalCommunicationType( NewNotificationRequestV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				.subject( "subject_length" )
				.build();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber( "protocol_number" ).build();

		// When
		Mockito.when(deliveryService.receiveNotification(
						Mockito.anyString(),
						Mockito.any( NewNotificationRequestV21.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull())
				).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri("/delivery/v2.1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV21.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
						Mockito.anyString(),
						Mockito.any( NewNotificationRequestV21.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull());
	}

	@Test
	void postPresignedUploadNotSuccess() {
		// Given
		List<PreLoadRequest> requests = new ArrayList<>();
		requests.add( PreLoadRequest.builder()
				.sha256( FILE_SHA_256 )
				.build());
		PnInvalidInputException pn = mock(PnInvalidInputException.class);

		// When
		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( 0 );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreLoadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus().isBadRequest();

	}

	@Test
	void postPresignedUploadSuccess() {
		// Given
		List<PreLoadRequest> requests = new ArrayList<>();
		requests.add( PreLoadRequest.builder()
				.sha256( FILE_SHA_256 )
				.build());
		List<PreLoadResponse> responses = new ArrayList<>();
		responses.add( PreLoadResponse.builder()
				.secret( SECRET )
				.httpMethod( PreLoadResponse.HttpMethodEnum.PUT )
				.url( URL )
				.build());


		// When
		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );
		Mockito.when(attachmentService.preloadDocuments( Mockito.anyList() ))
				.thenReturn( responses );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreLoadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus().isOk();

		Mockito.verify( attachmentService ).preloadDocuments( requests );
	}

	@Test
	void postPresignedUploadFailure() {
		//GIven
		List<PreloadRequest> requests = new ArrayList<>();
		requests.add( PreloadRequest.builder()
				.build());
		requests.add( PreloadRequest.builder()
				.build());
		requests.add( PreloadRequest.builder()
				.build());

		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreloadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header( PnDeliveryRestConstants.UID_HEADER, "uid" )
				.header( PnDeliveryRestConstants.CX_TYPE_HEADER, "PF" )
				.exchange()
				.expectStatus().isBadRequest();

	}

}
