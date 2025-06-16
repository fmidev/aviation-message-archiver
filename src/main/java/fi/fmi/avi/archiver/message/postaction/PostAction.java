package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public interface PostAction {

    void execute(PostActionContext context, ArchiveAviationMessage archiveMessage);

}
