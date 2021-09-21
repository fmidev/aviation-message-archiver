package fi.fmi.avi.archiver.spring.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class MapValuesToCollectionConverterTest {
    private AutoCloseable mockSession;
    @Mock
    private ConversionService conversionService;
    private MapValuesToCollectionConverter converter;

    @BeforeEach
    void setUp() {
        mockSession = MockitoAnnotations.openMocks(this);
        converter = new MapValuesToCollectionConverter(conversionService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockSession.close();
    }

    @Test
    void matches_returns_true_when_converting_from_Map_to_Collection_and_ConversioService_can_convert() {
        final TypeDescriptor sourceValueElementType = TypeDescriptor.valueOf(Long.class);
        final TypeDescriptor sourceType = TypeDescriptor.map(HashMap.class, TypeDescriptor.valueOf(Integer.class), sourceValueElementType);
        final TypeDescriptor sourceValuesType = TypeDescriptor.collection(Collection.class, sourceValueElementType);
        final TypeDescriptor targetType = TypeDescriptor.collection(ArrayList.class, TypeDescriptor.valueOf(String.class));
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(false);
        when(conversionService.canConvert(sourceValuesType, targetType)).thenReturn(true);
        assertThat(converter.matches(sourceType, targetType)).isTrue();
    }

    @Test
    void matches_returns_false_when_converting_from_Map_to_Collection_but_ConversioService_cannot_convert() {
        final TypeDescriptor sourceValueElementType = TypeDescriptor.valueOf(Long.class);
        final TypeDescriptor sourceType = TypeDescriptor.map(HashMap.class, TypeDescriptor.valueOf(Integer.class), sourceValueElementType);
        final TypeDescriptor targetType = TypeDescriptor.collection(ArrayList.class, TypeDescriptor.valueOf(String.class));
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(false);
        assertThat(converter.matches(sourceType, targetType)).isFalse();
    }

    @Test
    void matches_returns_false_when_converting_from_other_than_Map_to_Collection() {
        final TypeDescriptor targetType = TypeDescriptor.collection(ArrayList.class, TypeDescriptor.valueOf(String.class));
        final TypeDescriptor sourceType = targetType;
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(true);
        assertThat(converter.matches(sourceType, targetType)).isFalse();
    }

    @Test
    void matches_returns_false_when_converting_from_Map_to_other_than_Collection() {
        final TypeDescriptor sourceType = TypeDescriptor.map(HashMap.class, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Long.class));
        final TypeDescriptor targetType = sourceType;
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(true);
        assertThat(converter.matches(sourceType, targetType)).isFalse();
    }

    @Test
    void convert_delegates_to_ConversionService() {
        final TypeDescriptor sourceValueElementType = TypeDescriptor.valueOf(Long.class);
        final TypeDescriptor sourceType = TypeDescriptor.map(HashMap.class, TypeDescriptor.valueOf(Integer.class), sourceValueElementType);
        final TypeDescriptor sourceValuesType = TypeDescriptor.collection(Collection.class, sourceValueElementType);
        final TypeDescriptor targetType = TypeDescriptor.collection(ArrayList.class, TypeDescriptor.valueOf(String.class));

        final HashMap<Integer, Long> sourceMap = new HashMap<>();
        sourceMap.put(1, 2L);
        final ArrayList<String> expectedResult = new ArrayList<>();
        expectedResult.add("2");

        when(conversionService.convert(sourceMap.values(), sourceValuesType, targetType)).thenReturn(expectedResult);
        assertThat(converter.convert(sourceMap, sourceType, targetType)).isEqualTo(expectedResult);
    }

    @Test
    void convert_delegates_to_ConversionService_for_conversion_of_nested_types() {
        final TypeDescriptor sourceValueElementType = TypeDescriptor.collection(ArrayList.class, TypeDescriptor.valueOf(Long.class));
        final TypeDescriptor sourceType = TypeDescriptor.map(HashMap.class, TypeDescriptor.collection(HashSet.class, TypeDescriptor.valueOf(Integer.class)),
                sourceValueElementType);
        final TypeDescriptor sourceValuesType = TypeDescriptor.collection(Collection.class, sourceValueElementType);
        final TypeDescriptor targetType = TypeDescriptor.collection(ArrayList.class,
                TypeDescriptor.collection(HashSet.class, TypeDescriptor.valueOf(String.class)));

        final HashMap<HashSet<Integer>, ArrayList<Long>> sourceMap = new HashMap<>();
        final HashSet<Integer> key = new HashSet<>(Collections.singletonList(1));
        final ArrayList<Long> value = new ArrayList<>(Collections.singletonList(2L));
        sourceMap.put(key, value);
        final ArrayList<HashSet<String>> expectedResult = new ArrayList<>();
        final HashSet<String> resultElement = new HashSet<>(Collections.singletonList("2"));
        expectedResult.add(resultElement);

        when(conversionService.convert(sourceMap.values(), sourceValuesType, targetType)).thenReturn(expectedResult);
        assertThat(converter.convert(sourceMap, sourceType, targetType)).isEqualTo(expectedResult);
    }
}
