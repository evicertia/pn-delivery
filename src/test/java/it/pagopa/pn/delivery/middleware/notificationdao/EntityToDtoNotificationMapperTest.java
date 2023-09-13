package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotificationV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;


class EntityToDtoNotificationMapperTest {

    private EntityToDtoNotificationMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new EntityToDtoNotificationMapper();
    }

    @Test
    void throwEmptyDocument(){
        NotificationEntity notificationEntity = newNotificationEntity();
        notificationEntity.setPhysicalCommunicationType(null);
        assertThrows(PnInternalException.class, () -> mapper.entity2Dto(notificationEntity));
    }

    @Test
    void entity2DtoSuccess() {
        // Given
        NotificationEntity notificationEntity = newNotificationEntity();

        // When
        InternalNotification internalNotification = mapper.entity2Dto(notificationEntity);

        // Then
        Assertions.assertNotNull( internalNotification );
        Assertions.assertEquals( "noticeCode" ,internalNotification.getRecipients().get( 0 ).getPayments().get(0).getNoticeCode() );
        Assertions.assertEquals( "noticeCode_opt" ,internalNotification.getRecipients().get( 0 ).getPayments().get(0).getNoticeCodeAlternative() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa() );
        Assertions.assertNull( internalNotification.getRecipients().get( 1 ).getPayments().get(0).getNoticeCodeAlternative() );
        Assertions.assertNull( internalNotification.getRecipients().get( 1 ).getPayments().get(0).getPagoPa() );
    }

    private NotificationEntity newNotificationEntity() {
        NotificationRecipientEntity notificationRecipientEntity = NotificationRecipientEntity.builder()
                .recipientType(RecipientTypeEntity.PF)
                .recipientId("recipientTaxId")
                .digitalDomicile(NotificationDigitalAddressEntity.builder()
                        .address("address@pec.it")
                        .type(DigitalAddressTypeEntity.PEC)
                        .build())
                .denomination("recipientDenomination")
                .payments( List.of(
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("creditorTaxId")
                                        .noticeCode("noticeCode")
                                        .pagoPaForm(PagoPaPaymentEntity.builder()
                                                .contentType("application/pdf")
                                                .digests(NotificationAttachmentDigestsEntity.builder()
                                                        .sha256("sha256")
                                                        .build())
                                                .ref(NotificationAttachmentBodyRefEntity.builder()
                                                        .key("key")
                                                        .versionToken("versionToken")
                                                        .build())
                                                .build())
                                        .build(),
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("creditorTaxId")
                                        .noticeCode("noticeCode_opt")
                                        .build()
                        )
                )
                .physicalAddress(NotificationPhysicalAddressEntity.builder()
                        .address("address")
                        .addressDetails("addressDetail")
                        .zip("zip")
                        .at("at")
                        .municipality("municipality")
                        .province("province")
                        .municipalityDetails("municipalityDetails")
                        .build())
                .build();
        NotificationRecipientEntity notificationRecipientEntity1 = NotificationRecipientEntity.builder()
                .recipientType(RecipientTypeEntity.PF)
                .payments( List.of(
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("77777777777")
                                        .noticeCode("002720356512737953")
                                        .build()
                        )
                )
                .physicalAddress(NotificationPhysicalAddressEntity.builder()
                        .foreignState("Svizzera")
                        .address("via canton ticino")
                        .at("presso")
                        .addressDetails("19")
                        .municipality("cCantonticino")
                        .province("cantoni")
                        .zip("00100")
                        .municipalityDetails("frazione1")
                        .build())
                .recipientId( "fakeRecipientId" )
                .build();
        return NotificationEntity.builder()
                .documents((List.of(DocumentAttachmentEntity
                        .builder()
                        .digests(NotificationAttachmentDigestsEntity.builder()
                                .sha256("jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=")
                                .build())
                        .ref(NotificationAttachmentBodyRefEntity.builder()
                                .key("KEY")
                                .versionToken("versioneToken")
                                .build())
                        .build())))
                .iun("IUN_01")
                .notificationAbstract( "Abstract" )
                .idempotenceToken( "idempotenceToken" )
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(FullSentNotificationV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .group( "Group_1" )
                .sentAt( Instant.now() )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .pagoPaIntMode( FullSentNotificationV21.PagoPaIntModeEnum.NONE.getValue() )
                .recipients( List.of(notificationRecipientEntity, notificationRecipientEntity1) )
                .version( 1 )
                .build();
    }

}