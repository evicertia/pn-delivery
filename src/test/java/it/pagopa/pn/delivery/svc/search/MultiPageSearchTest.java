package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class MultiPageSearchTest {

    private NotificationDao notificationDao;
    private InputSearchNotificationDto inputSearchNotificationDto;
    private PnDeliveryConfigs cfg;

    @BeforeEach
    void setup() {
        this.notificationDao = new NotificationDaoMock();
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        this.inputSearchNotificationDto = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .size( 10 )
                .build();
    }

    @Test
    void searchNotificationMetadata() {
        MultiPageSearch multiPageSearch = new MultiPageSearch(
                notificationDao,
                inputSearchNotificationDto,
                null,
                cfg);

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = multiPageSearch.searchNotificationMetadata();

        Assertions.assertNotNull( result );
    }

    private static class NotificationDaoMock implements NotificationDao {

        private final EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

        private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void addNotification(Notification notification) throws IdConflictException {

        }

        @Override
        public Optional<Notification> getNotificationByIun(String iun) {
            return Optional.empty();
        }

        @Override
        public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {

            return ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                    .result(Collections.singletonList( NotificationSearchRow.builder()
                            .iun( "IUN" )
                            .group( "GRP" )
                            .paNotificationId( "PaNotificationId" )
                            .notificationStatus( NotificationStatus.VIEWED )
                            .senderId( "SenderId" )
                            .subject( "Subject" )
                            .build() ))
                    .moreResult( false )
                    .build();
        }
    }
}