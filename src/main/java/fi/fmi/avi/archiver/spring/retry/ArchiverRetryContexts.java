package fi.fmi.avi.archiver.spring.retry;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

import static fi.fmi.avi.archiver.spring.retry.RetryContextAttributeAccessor.builder;

public final class ArchiverRetryContexts {
    public static final String RETRY_COUNT_LOGNAME = "retryCount";
    public static final String RETRY_ACTION = "retryAction";
    public static final RetryContextAttributeAccessor<ReadableLoggingContext> LOGGING_CONTEXT = builder(ReadableLoggingContext.class)//
            .setNameFromType()//
            .setDefaultValue(NoOpLoggingContext.getInstance())//
            .setDoOnSet(ReadableLoggingContext::readableCopy)//
            .build();
    public static final RetryContextAttributeAccessor<String> DATABASE_OPERATION = builder(String.class)//
            .setName(DatabaseAccess.class.getName() + ".databaseOperation")//
            .setDefaultValue("")//
            .build();

    private ArchiverRetryContexts() {
        throw new AssertionError();
    }
}
