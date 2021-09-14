package fi.fmi.avi.archiver.initializing;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.AbstractMessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.archiver.message.populator.ReflectionMessagePopulatorFactory;

@SpringBootTest({ "auto.startup=false", "testclass.name=fi.fmi.avi.archiver.initializing.MessagePopulatorExecutionChainHolderTest" })
@ContextConfiguration(classes = { AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigDataApplicationContextInitializer.class })
@Sql(scripts = { "classpath:/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ActiveProfiles("MessagePopulatorExecutionChainHolderTest")
class MessagePopulatorExecutionChainHolderTest {
    @Autowired
    private MessagePopulatorExecutionChainHolder messagePopulatorExecutionChainHolder;

    @Autowired
    private MessagePopulatorService messagePopulatorService;

    @Qualifier("messagePopulators")
    @Autowired
    private List<MessagePopulator> messagePopulators;

    @Test
    void testMessagePopulatorExecutionChain() {
        // TODO: Remove debug output
        System.out.println("*** EXECUTION CHAIN ***");
        messagePopulatorExecutionChainHolder.getExecutionChain().forEach(System.out::println);
        System.out.println("*** POPULATORS ***");
        messagePopulators.forEach(System.out::println);
        // End debug output

        final List<InputAviationMessage> inputMessages = Collections.singletonList(InputAviationMessage.builder().buildPartial());
        final List<ArchiveAviationMessage> result = messagePopulatorService.populateMessages(inputMessages, new MessageHeaders(Collections.emptyMap()))//
                .getPayload();

        final ArchiveAviationMessage expected = ArchiveAviationMessage.builder()//
                .setRoute(1)//
                .setFormat(2)//
                .setType(4)//
                .setMessageTime(Instant.parse("2001-02-03T04:05:06.019Z"))//
                .setIcaoAirportCode("EFXX")//
                .setStationId(1)//
                .setMessage("testMessage")//
                .setValidFrom(Instant.parse("2001-02-04T05:06:07.029Z"))//
                .setValidTo(Instant.parse("2001-02-05T06:07:08.041Z"))//
                .build();
        assertThat(result).containsExactly(expected);
    }

    public static class FixedValueTestPopulator1 implements MessagePopulator {
        private Map<IdCategory, Integer> ids = Collections.emptyMap();
        private Instant messageTime = Instant.EPOCH;

        private List<Instant> validityPeriod = Collections.emptyList();

        @Override
        public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
            requireNonNull(input, "input");
            requireNonNull(builder, "builder");
            if (ids.containsKey(IdCategory.ROUTE)) {
                builder.setRoute(ids.get(IdCategory.ROUTE));
            }
            if (ids.containsKey(IdCategory.FORMAT)) {
                builder.setFormat(ids.get(IdCategory.FORMAT));
            }
            if (ids.containsKey(IdCategory.TYPE)) {
                builder.setType(ids.get(IdCategory.TYPE));
            }
            if (!messageTime.equals(Instant.EPOCH)) {
                builder.setMessageTime(messageTime);
            }
            if (validityPeriod.size() >= 1 && validityPeriod.get(0) != null) {
                builder.setValidFrom(validityPeriod.get(0));
            }
            if (validityPeriod.size() >= 2 && validityPeriod.get(1) != null) {
                builder.setValidTo(validityPeriod.get(1));
            }
        }

        public void setIds(final Map<IdCategory, Integer> ids) {
            this.ids = requireNonNull(ids, "ids");
        }

        public void setMessageTime(final Instant messageTime) {
            this.messageTime = requireNonNull(messageTime, "messageTime");
        }

        public void setValidityPeriod(final List<Instant> validityPeriod) {
            requireNonNull(validityPeriod, "validityPeriod");
            if (validityPeriod.size() > 2) {
                throw new IllegalArgumentException("too many instants: " + validityPeriod);
            }
            this.validityPeriod = validityPeriod;

        }

        @Override
        public String toString() {
            return "FixedValueTestPopulator1{" + "ids=" + ids + ", messageTime=" + messageTime + ", validityPeriod=" + validityPeriod + '}';
        }

        public enum IdCategory {
            ROUTE, FORMAT, TYPE
        }
    }

    public static class FixedValueTestPopulator2 implements MessagePopulator {
        private String station = "";
        private String message = "";

        @Override
        public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
            requireNonNull(input, "input");
            requireNonNull(builder, "builder");
            if (!station.isEmpty()) {
                builder.setIcaoAirportCode(station);
            }
            if (!message.isEmpty()) {
                builder.setMessage(message);
            }
        }

        public void setStation(final String station) {
            this.station = requireNonNull(station, "station");
        }

        public void setMessage(final String message) {
            this.message = requireNonNull(message, "message");
        }

        @Override
        public String toString() {
            return "FixedValueTestPopulator2{" + "station='" + station + '\'' + ", message='" + message + '\'' + '}';
        }
    }

    @Configuration
    @Profile("MessagePopulatorExecutionChainHolderTest")
    static class MessagePopulatorExecutionChainHolderTestConfig {
        @Autowired
        private AbstractMessagePopulatorFactory.PropertyConverter messagePopulatorFactoryPropertyConverter;

        @Bean
        public MessagePopulatorFactory<FixedValueTestPopulator1> fixedRouteFormatTypeTestPopulatorFactory() {
            return new ReflectionMessagePopulatorFactory<>(messagePopulatorFactoryPropertyConverter, FixedValueTestPopulator1.class);
        }

        @Bean
        public MessagePopulatorFactory<FixedValueTestPopulator2> fixedStationTestPopulatorFactory() {
            return new ReflectionMessagePopulatorFactory<>(messagePopulatorFactoryPropertyConverter, FixedValueTestPopulator2.class);
        }
    }
}
