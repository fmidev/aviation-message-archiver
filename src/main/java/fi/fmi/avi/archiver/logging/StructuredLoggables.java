package fi.fmi.avi.archiver.logging;

import java.util.Locale;

import fi.fmi.avi.archiver.util.GeneratedClasses;

final class StructuredLoggables {
    private StructuredLoggables() {
        throw new AssertionError();
    }

    /**
     * Return default {@link StructuredLoggable#getStructureName() structure name} for a {@code StructuredLoggable} class.
     *
     * @param forClass
     *         class to get default structure name for
     *
     * @return default structure name for profided {@code forClass}
     */
    public static String defaultStructureName(final Class<? extends StructuredLoggable> forClass) {
        final Class<? extends StructuredLoggable> classForName = resolveClassForStructureName(forClass);
        final String simpleName = classForName.getSimpleName();
        if (simpleName.isEmpty()) {
            final String className = classForName.getName();
            return startingWithLowerCase(className.substring(className.lastIndexOf('.') + 1).replace('$', '_'));
        } else {
            return startingWithLowerCase(simpleName);
        }
    }

    /**
     * Resolve class to use for structure name.
     * If provided {@code forClass} is known to be a generated class, this method resolves the class declaring it.
     *
     * <p>
     * Implementation note: this implementation <strong>does not handle FreeBuilder builders</strong> correctly, but does handle values and partials.
     * Though, builders are not expected to implement {@code StructuredLoggable} interface.
     * </p>
     *
     * @param forClass
     *         class to resolve structure name class for
     *
     * @return structure name class
     */
    private static Class<? extends StructuredLoggable> resolveClassForStructureName(final Class<? extends StructuredLoggable> forClass) {
        Class<? extends StructuredLoggable> classForName = forClass;
        Class<?> superclass = classForName.getSuperclass();
        while (superclass != null //
                && StructuredLoggable.class.isAssignableFrom(superclass) //
                && GeneratedClasses.isKnownGenerated(classForName)) {
            //noinspection unchecked
            classForName = (Class<? extends StructuredLoggable>) superclass;
            superclass = classForName.getSuperclass();
        }
        return classForName;
    }

    private static String startingWithLowerCase(final String string) {
        final int length = string.length();
        if (length > 1) {
            return Character.toLowerCase(string.charAt(0)) + string.substring(1);
        } else if (length == 1) {
            return string.toLowerCase(Locale.ROOT);
        } else {
            return string;
        }
    }
}
