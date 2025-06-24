package fi.fmi.avi.archiver.message.processor.postaction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class TestPostActionRegistry {
    private final Map<String, TestPostAction> postActions = new ConcurrentHashMap<>();

    public void register(final TestPostAction postAction) {
        requireNonNull(postAction, "postAction");
        @Nullable final TestPostAction previouslyRegistered = postActions.putIfAbsent(postAction.getId(), postAction);
        if (previouslyRegistered != null) {
            throw new IllegalArgumentException("Duplicate TestPostAction id: <" + postAction.getId() + ">");
        }
    }

    public TestPostAction get(final String id) {
        return getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("No TestPostAction registered with id <" + id + ">"));
    }

    public TestPostAction get(final TestPostAction.EnumId enumId) {
        requireNonNull(enumId, "enumId");
        return get(enumId.getId());
    }

    public Optional<TestPostAction> getOptional(final String id) {
        requireNonNull(id, "id");
        return Optional.ofNullable(postActions.get(id));
    }

    public Optional<TestPostAction> getOptional(final TestPostAction.EnumId enumId) {
        requireNonNull(enumId, "enumId");
        return getOptional(enumId.getId());
    }

    public List<TestPostAction> getAll() {
        return List.copyOf(postActions.values());
    }

    public void resetAll() {
        postActions.values().forEach(TestPostAction::reset);
    }
}
