package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.evinotice.v2.dto.EviNotice;
import it.pagopa.pn.delivery.generated.openapi.msclient.evinotice.v2.dto.EviNoticeResponse;

import it.pagopa.pn.delivery.utils.DataUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Autowired;

import reactor.netty.http.client.HttpClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.OffsetDateTime;
import java.util.*;

import javax.net.ssl.KeyManagerFactory;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;

@Slf4j
@Service
public class StatusService {

    private final NotificationDao notificationDao;
    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    private final NotificationDelegatedService notificationDelegatedService;
    private final NotificationCostEntityDao notificationCostEntityDao;

    private final PnDeliveryConfigs pnDeliveryConfig;

    private final PnExternalRegistriesClientImpl externalRegistriesClient;

    public StatusService(NotificationDao notificationDao,
                        NotificationMetadataEntityDao notificationMetadataEntityDao,
                        NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao,
                        NotificationDelegatedService notificationDelegatedService,
                        NotificationCostEntityDao notificationCostEntityDao,
                        PnExternalRegistriesClientImpl externalRegistriesClient,
                        PnDeliveryConfigs pnDeliveryConfig) {
        this.notificationDao = notificationDao;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.notificationDelegationMetadataEntityDao = notificationDelegationMetadataEntityDao;
        this.notificationDelegatedService = notificationDelegatedService;
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.pnDeliveryConfig = pnDeliveryConfig;
        this.externalRegistriesClient = externalRegistriesClient;

    }

    public void updateStatus(RequestUpdateStatusDto dto) {
        Optional<InternalNotification> notificationOptional = notificationDao.getNotificationByIun(dto.getIun(), true);

        if (notificationOptional.isPresent()) {
            InternalNotification notification = notificationOptional.get();
            log.info("Notification with protocolNumber={} and iun={} is present", notification.getPaProtocolNumber(), dto.getIun());

            NotificationStatus nextStatus = dto.getNextStatus();
            OffsetDateTime acceptedAt;
            switch (nextStatus) {
                case ACCEPTED -> {
                    acceptedAt = dto.getTimestamp();
                    putNotificationMetadata(dto, notification, acceptedAt);

                    log.info("Sending EviNotice when DigitalAddress.Type == EVINOTICE with protocolNumber={} and iun={} is present", notification.getPaProtocolNumber(), dto.getIun());

                    notification.getRecipients().forEach(recipient -> {
                        if (NotificationDigitalAddress.TypeEnum.EVINOTICE.equals(recipient.getDigitalDomicile().getType())) {
                            sendEviNotice(notification, recipient.getDigitalDomicile().getAddress());
                        }
                    });
                }
                case REFUSED ->
                        notification.getRecipients().stream()
                                .filter(r -> Objects.nonNull(r.getPayments()))
                                .forEach(r -> r.getPayments().forEach(notificationPaymentInfo -> {
                                    if(notificationPaymentInfo.getPagoPa() != null && StringUtils.hasText(notificationPaymentInfo.getPagoPa().getNoticeCode())){
                                        notificationCostEntityDao.deleteItem(NotificationCostEntity.builder()
                                                .creditorTaxIdNoticeCode(notificationPaymentInfo.getPagoPa().getCreditorTaxId() +"##"+ notificationPaymentInfo.getPagoPa().getNoticeCode())
                                                .build());
                                    }
                                }));
                default -> {
                    Key key = Key.builder()
                            .partitionValue( notification.getIun() + "##" + notification.getRecipients().get( 0 ).getInternalId() )
                            .sortValue( notification.getSentAt().toString() )
                            .build();
                    Optional<NotificationMetadataEntity> optMetadata = notificationMetadataEntityDao.get( key );
                    if (optMetadata.isPresent() ) {
                        acceptedAt = OffsetDateTime.parse( optMetadata.get().getTableRow().get( "acceptedAt" ) );
                        putNotificationMetadata(dto, notification, acceptedAt);
                    } else {
                        log.debug( "Unable to retrieve accepted date - iun={} recipientId={}", notification.getIun(), notification.getRecipientIds().get( 0 ) );
                    }
                }
            }

        } else {
            throw new PnInternalException("Try to update status for non existing iun=" + dto.getIun(),
                    ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
        }
    }

    private void putNotificationMetadata(RequestUpdateStatusDto dto, InternalNotification notification, OffsetDateTime acceptedAt) {
        List<NotificationMetadataEntity> nextMetadataEntry = computeMetadataEntry(dto, notification, acceptedAt);
        nextMetadataEntry.forEach(metadata -> {
            notificationMetadataEntityDao.put(metadata);
            List<NotificationDelegationMetadataEntity> delegationMetadata = notificationDelegatedService.computeDelegationMetadataEntries(metadata);
            delegationMetadata.forEach(notificationDelegationMetadataEntityDao::put);
        });
    }

    private List<NotificationMetadataEntity> computeMetadataEntry(RequestUpdateStatusDto dto, InternalNotification notification, OffsetDateTime acceptedAt) {
        NotificationStatus lastStatus = dto.getNextStatus();
        String rootSenderId = externalRegistriesClient.getRootSenderId(notification.getSenderPaId());
        // FIXME: This works on console but not debugging.. trying to now why
        String creationMonth = "202403"; // DataUtils.extractCreationMonth( notification.getSentAt().toInstant() );

        List<String> opaqueTaxIds = notification.getRecipientIds();

        return opaqueTaxIds.stream()
                    .map( recipientId -> this.buildOneSearchMetadataEntry( notification, lastStatus, recipientId, opaqueTaxIds, creationMonth, acceptedAt, rootSenderId))
                    .toList();
    }

    private NotificationMetadataEntity buildOneSearchMetadataEntry(
            InternalNotification notification,
            NotificationStatus lastStatus,
            String recipientId,
            List<String> recipientsIds,
            String creationMonth,
            OffsetDateTime acceptedAt,
            String rootSenderId
    ) {
        int recipientIndex = recipientsIds.indexOf( recipientId );

        Map<String,String> tableRowMap = createTableRowMap(notification, lastStatus, recipientsIds, acceptedAt);

        return NotificationMetadataEntity.builder()
                .notificationStatus( lastStatus.toString() )
                .senderId( notification.getSenderPaId() )
                .rootSenderId(rootSenderId)
                .recipientId( recipientId )
                .sentAt( notification.getSentAt().toInstant() )
                .notificationGroup( notification.getGroup() )
                .recipientIds( recipientsIds )
                .tableRow( tableRowMap )
                .senderIdRecipientId( DataUtils.createConcatenation( notification.getSenderPaId(), recipientId  ) )
                .senderIdCreationMonth( DataUtils.createConcatenation( notification.getSenderPaId(), creationMonth ) )
                .recipientIdCreationMonth( DataUtils.createConcatenation( recipientId , creationMonth ) )
                .iunRecipientId( DataUtils.createConcatenation( notification.getIun(), recipientId ) )
                .recipientOne( recipientIndex <= 0 )
                .build();
    }

    @NotNull
    private Map<String, String> createTableRowMap(InternalNotification notification, NotificationStatus lastStatus, List<String> recipientsIds, OffsetDateTime acceptedAt) {
        Map<String,String> tableRowMap = new HashMap<>();
        tableRowMap.put( "iun", notification.getIun() );
        tableRowMap.put( "recipientsIds", recipientsIds.toString() );
        tableRowMap.put( "paProtocolNumber", notification.getPaProtocolNumber() );
        tableRowMap.put( "subject", notification.getSubject() );
        tableRowMap.put( "senderDenomination", notification.getSenderDenomination() );
        if ( Objects.nonNull( acceptedAt )) {
            tableRowMap.put( "acceptedAt", acceptedAt.toString() );
        }
        return tableRowMap;
    }

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse, String errorMessagePrefix) {

        return clientResponse.bodyToMono(String.class)
            .flatMap(errorBody -> {
                String errorMessage = errorMessagePrefix + errorBody;
                log.error(errorMessage);
                return Mono.<Throwable>error(new RuntimeException(errorMessage));
            });
    }

    private void sendEviNotice(InternalNotification notification, String address)
    {
        WebClient webClient = createEviNoticeClient(pnDeliveryConfig);
        EviNotice eviNoticeDTO = new EviNotice();

        String body = notification.getSubject() + "<br><br>You can check the notification at the following link : <br>";
        body += "<a href='https://cittadini.dev.notifichedigitali.it/notifiche/" + notification.getPaProtocolNumber() + "/dettaglio'>See at PagoPA</a>";
        eviNoticeDTO.setSubject("New PagoPA Notification received");
        eviNoticeDTO.setBody(body);
        eviNoticeDTO.setCertificationLevel("QERDS");
        eviNoticeDTO.setQeRDSEnrollmentProfile(pnDeliveryConfig.getEviNoticeQERDSProfileByEmail());
        eviNoticeDTO.setQeRDSEnrollmentAllowed("true");
        eviNoticeDTO.setRecipientLegalIdRequired("true");
        eviNoticeDTO.setRecipientAddress(address);
        eviNoticeDTO.setAffidavitKinds(Arrays.asList("SubmittedAdvanced", "Read", "Refused", "OnDemand", "CompleteAdvanced"));
        eviNoticeDTO.setDeliverySignFixedEmail(address);
        eviNoticeDTO.setEvidenceAccessControlMethod("Public");
        eviNoticeDTO.setDeliverySignMethod("EmailPin");
        eviNoticeDTO.setCommitmentChoice("Disabled");
        eviNoticeDTO.setCommitmentCommentsAllowed("false");

        log.info( "Trying to Post Evinotice /Submit notification " + notification.getIun()) ;

        webClient.post()
            .uri("/EviNotice/Submit")
            .bodyValue(eviNoticeDTO)
            .retrieve()
            .onStatus(HttpStatus.BAD_REQUEST::equals, clientResponse -> handleError(clientResponse, "Problems trying to submit the EviNotice: "))
            .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, clientResponse -> handleError(clientResponse, "Internal Server Error: "))
            .bodyToMono(EviNoticeResponse.class)
            .doOnSuccess(evResponse -> {
                log.info( "EviNotice Submitted with Id => " + evResponse.getId()) ;
            })
            .onErrorMap(Throwable.class, throwable -> new Exception("plain exception"))
            .subscribe();
    }

    WebClient createEviNoticeClient(PnDeliveryConfigs pnDeliveryConfig) {
        try {
            log.info( "Adding SSLContext to WebClient ");
            HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(getSslContextByPk12(pnDeliveryConfig)));
            ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

            log.info( "Build EviNotice Client " + pnDeliveryConfig.getClientEviNoticeBasepath());
            WebClient client = WebClient.builder()
                .baseUrl(pnDeliveryConfig.getClientEviNoticeBasepath())
                .clientConnector(connector)
                .defaultHeaders(header -> header.
                    setBasicAuth(pnDeliveryConfig.getClientEviNoticeUserName(), pnDeliveryConfig.getClientEviNoticePass()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

            return client;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static SslContext getSslContextByPk12(PnDeliveryConfigs pnDeliveryConfig) {
        try {
            // Load Cert
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream file =  new FileInputStream(pnDeliveryConfig.getEviNoticeCertificateFile());
            keyStore.load(file, pnDeliveryConfig.getEviNoticeCertificatePin().toCharArray());
            // Create store to load cert
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, pnDeliveryConfig.getEviNoticeCertificatePin().toCharArray());
            // Create SSL context
            return SslContextBuilder.forClient()
                    .keyManager(keyManagerFactory)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}