package fi.fmi.avi.archiver.initializing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

@SpringBootTest({ "auto.startup=false", "testclass.name=fi.fmi.avi.archiver.initializing.AviationProductsHolderTest" })
@ContextConfiguration(classes = { AviationMessageArchiver.class, TestConfig.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigDataApplicationContextInitializer.class })
class AviationProductsHolderTest {
    @Resource(name = "messageRouteIds")
    private Map<String, Integer> messageRouteIds;
    @Resource(name = "messageFormatIds")
    private Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Test
    public void product_routes_have_id() {
        final Map<String, Integer> actualRouteIds = aviationProductsHolder.getProducts()
                .stream()
                .collect(Collectors.toMap(product -> product.getId() + ":" + product.getRoute(), AviationProductsHolder.AviationProduct::getRouteId));
        assertThat(actualRouteIds)//
                .isNotEmpty()//
                .allSatisfy((productId, routeId) -> assertThat(routeId).as(productId).isGreaterThanOrEqualTo(0));
    }

    @Test
    public void product_routes_have_id_equal_to_route_table() {
        final List<Map.Entry<String, Integer>> actualRouteIds = aviationProductsHolder.getProducts()
                .stream()
                .map(product -> new SimpleImmutableEntry<>(product.getRoute(), product.getRouteId()))
                .collect(Collectors.toList());
        assertThat(actualRouteIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry).isIn(messageRouteIds.entrySet()));
    }

    @Test
    public void product_file_message_formats_have_id() {
        final List<Map.Entry<String, Integer>> actualFormatIds = aviationProductsHolder.getProducts().stream()//
                .flatMap(product -> product.getFileConfigs()
                        .stream()
                        .map(file -> new SimpleImmutableEntry<>(product.getId() + ":" + file.getFormat(), file.getFormatId())))//
                .collect(Collectors.toList());
        assertThat(actualFormatIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry.getValue()).as(entry.getKey()).isGreaterThanOrEqualTo(0));
    }

    @Test
    public void product_file_message_formats_have_id_equal_to_format_table() {
        final List<Map.Entry<GenericAviationWeatherMessage.Format, Integer>> actualFormatIds = aviationProductsHolder.getProducts().stream()//
                .flatMap(product -> product.getFileConfigs().stream().map(file -> new SimpleImmutableEntry<>(file.getFormat(), file.getFormatId())))//
                .collect(Collectors.toList());
        assertThat(actualFormatIds)//
                .isNotEmpty()//
                .allSatisfy(entry -> assertThat(entry).isIn(messageFormatIds.entrySet()));
    }
}
