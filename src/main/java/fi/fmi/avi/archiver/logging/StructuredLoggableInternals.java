package fi.fmi.avi.archiver.logging;

import java.util.Locale;

import fi.fmi.avi.archiver.util.GeneratedClasses;

final class StructuredLoggableInternals {
    private StructuredLoggableInternals() {
        throw new AssertionError();
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
    static Class<? extends StructuredLoggable> resolveClassForStructureName(final Class<? extends StructuredLoggable> forClass) {
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

    static String startingWithLowerCase(final String string) {
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
