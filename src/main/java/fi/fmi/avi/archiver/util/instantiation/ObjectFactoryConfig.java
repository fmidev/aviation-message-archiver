package fi.fmi.avi.archiver.util.instantiation;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A marker interface for a typed configuration definition to be instantiated by an {@link ObjectFactoryConfigFactory}.
 *
 * <p>
 * Following rules apply to types implementing this interface, and all {@link ObjectFactoryConfigFactory}
 * implementations must support these rules.
 * </p>
 *
 * <ul>
 *     <li>
 *         Any valid configuration type must implement this interface.
 *     </li>
 *
 *     <li>
 *         All properties are mandatory by default. Only properties of type {@link Optional}, {@link OptionalInt},
 *         {@link OptionalLong} or {@link OptionalDouble} are considered optional. Optional properties may be omitted
 *         in the configuration input.
 *     </li>
 *
 *     <li>
 *         The configuration type may declare constants and methods with a body (e.g. default methods of an interface).
 *         Such type members shall be ignored by the {@code ObjectFactoryConfigFactory} implementation.
 *     </li>
 *
 *     <li>
 *         Methods with parameters are forbidden, unless a method body (implementation) is provided. These methods are
 *         not considered as configuration properties.
 *     </li>
 *
 *     <li>
 *         The configuration type may choose to declare properties using bean style naming (e.g.
 *         {@code getConfigValue()}, {@code isConfigBoolean()}) or without any prefixes (e.g. {@code configValue()}).
 *         However, these styles should not be mixed. It is up to {@code ObjectFactoryConfigFactory} implementation to
 *         decide whether it forbids mixing of different naming conventions or supports only one of them.
 *     </li>
 *
 *     <li>
 *         A configuration type may declare another {@code ObjectFactoryConfig} type as a property type. The
 *         {@code ObjectFactoryConfigFactory} implementation must support such nested {@code ObjectFactoryConfig} types.
 *         Though, circular references are forbidden. E.g. type {@code A} may not have a property of type {@code A},
 *         or type {@code A} may not have a property of type {@code B} which has a property of type {@code A}.
 *     </li>
 * </ul>
 */
public interface ObjectFactoryConfig {
}
