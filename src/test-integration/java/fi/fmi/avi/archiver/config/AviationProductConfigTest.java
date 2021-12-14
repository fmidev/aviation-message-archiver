package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.config.AviationProductConfigTest"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AviationProductConfigTest {

    private final Map<String, Integer> messageRouteIds;
    private final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;
    private final Map<String, AviationProduct> aviationProducts;

    public AviationProductConfigTest(final Map<String, Integer> messageRouteIds,
                                     final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds,
                                     final Map<String, AviationProduct> aviationProducts) {
        this.messageRouteIds = requireNonNull(messageRouteIds, "messageRouteIds");
        this.messageFormatIds = requireNonNull(messageFormatIds, "messageFormatIds");
        this.aviationProducts = requireNonNull(aviationProducts, "aviationProducts");
    }

    @Test
    void product_routes_have_id() {
        final Map<String, Integer> actualRouteIds = aviationProducts.values().stream()//
                .collect(Collectors.toMap(product -> product.getId() + ":" + product.getRoute(), AviationProduct::getRouteId));
        assertThat(actualRouteIds)//
                .isNotEmpty()//
                .allSatisfy((productId, routeId) -> assertThat(routeId).as(productId).isGreaterThanOrEqualTo(0));
    }

    @Test
    void product_routes_have_id_equal_to_route_table() {
        final List<Map.Entry<String, Integer>> actualRouteIds = aviationProducts.values().stream()//
                .map(product -> new SimpleImmutableEntry<>(product.getRoute(), product.getRouteId()))//
                .collect(Collectors.toList());
        assertThat(actualRouteIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry).isIn(messageRouteIds.entrySet()));
    }

    @Test
    void product_file_message_formats_have_id() {
        final List<Map.Entry<String, Integer>> actualFormatIds = aviationProducts.values().stream()//
                .flatMap(product -> product.getFileConfigs()
                        .stream()
                        .map(file -> new SimpleImmutableEntry<>(product.getId() + ":" + file.getFormat(), file.getFormatId())))//
                .collect(Collectors.toList());
        assertThat(actualFormatIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry.getValue()).as(entry.getKey()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void product_file_message_formats_have_id_equal_to_format_table() {
        final List<Map.Entry<GenericAviationWeatherMessage.Format, Integer>> actualFormatIds = aviationProducts.values().stream()//
                .flatMap(product -> product.getFileConfigs().stream().map(file -> new SimpleImmutableEntry<>(file.getFormat(), file.getFormatId())))//
                .collect(Collectors.toList());
        assertThat(actualFormatIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry).isIn(messageFormatIds.entrySet()));
    }
}
