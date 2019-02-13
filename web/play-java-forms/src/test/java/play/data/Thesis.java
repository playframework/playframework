/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.validation.Constraints;
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

import java.util.List;

public class Thesis {

    @Constraints.Required
    @Constraints.MinLength(10)
    private String title;

    @Constraints.Required
    private FilePart<TemporaryFile> document;

    @Constraints.Required
    private List<FilePart<TemporaryFile>> attachments;

    @Constraints.Required
    private List<FilePart<TemporaryFile>> bibliography;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FilePart<TemporaryFile> getDocument() {
        return document;
    }

    public void setDocument(FilePart<TemporaryFile> document) {
        this.document = document;
    }

    public List<FilePart<TemporaryFile>> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FilePart<TemporaryFile>> attachments) {
        this.attachments = attachments;
    }

    public List<FilePart<TemporaryFile>> getBibliography() {
        return bibliography;
    }

    public void setBibliography(List<FilePart<TemporaryFile>> bibliography) {
        this.bibliography = bibliography;
    }
}
