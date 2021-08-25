package fi.fmi.avi.archiver.message.populator;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

public final class MessagePopulatorTests {
    public static final Pattern FILE_NAME_PATTERN = Pattern.compile("(metar|taf|tca|speci|sigmet|vaa|airmet|swx)"
            + "(?:_(?:(?<yyyy>\\d{4})-)?(?:(?<MM>\\d{2})-)?(?<dd>\\d{2})?T(?<hh>\\d{2})?(?::(?<mm>\\d{2}))?(?::(?<ss>\\d{2}))?)?" + "(?:\\.txt|\\.xml)");
    public static final ArchiveAviationMessage EMPTY_RESULT = ArchiveAviationMessage.builder().buildPartial();
    public static final Map<GenericAviationWeatherMessage.Format, Integer> FORMAT_IDS = Arrays.stream(FormatId.values())//
            .collect(Maps.toImmutableEnumMap(FormatId::getFormat, FormatId::getId));
    public static final Map<MessageType, Integer> TYPE_IDS = Arrays.stream(TypeId.values())//
            .collect(ImmutableMap.toImmutableMap(TypeId::getType, TypeId::getId));

    private MessagePopulatorTests() {
        throw new AssertionError();
    }

    enum FormatId {
        TAC(GenericAviationWeatherMessage.Format.TAC, 1), //
        IWXXM(GenericAviationWeatherMessage.Format.IWXXM, 2), //
        ;

        private final GenericAviationWeatherMessage.Format format;
        private final int id;

        FormatId(final GenericAviationWeatherMessage.Format format, final int id) {
            this.format = format;
            this.id = id;
        }

        public static FormatId valueOf(final GenericAviationWeatherMessage.Format format) {
            return Arrays.stream(values())//
                    .filter(formatId -> formatId.getFormat() == format)//
                    .findAny()//
                    .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + format));
        }

        public GenericAviationWeatherMessage.Format getFormat() {
            return format;
        }

        public int getId() {
            return id;
        }
    }

    enum TypeId {
        SPECI(MessageType.SPECI, 1), //
        METAR(MessageType.METAR, 2), //
        TAF(MessageType.TAF, 3), //
        SIGMET(MessageType.SIGMET, 4), //
        AIRMET(MessageType.AIRMET, 5), //
        TCA(MessageType.TROPICAL_CYCLONE_ADVISORY, 6), //
        VAA(MessageType.VOLCANIC_ASH_ADVISORY, 7), //
        SWX(MessageType.SPACE_WEATHER_ADVISORY, 8), //
        ;

        private final MessageType type;
        private final int id;

        TypeId(final MessageType type, final int id) {
            this.type = type;
            this.id = id;
        }

        public static TypeId valueOf(final MessageType type) {
            return Arrays.stream(values())//
                    .filter(typeId -> typeId.getType() == type)//
                    .findAny()//
                    .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + type));
        }

        public MessageType getType() {
            return type;
        }

        public int getId() {
            return id;
        }
    }
}
