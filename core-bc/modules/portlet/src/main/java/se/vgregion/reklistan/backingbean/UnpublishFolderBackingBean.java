package se.vgregion.reklistan.backingbean;


import com.liferay.journal.model.JournalFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.exception.UnpublishFolderException;
import se.vgregion.reklistan.service.FolderService;
import se.vgregion.reklistan.util.FacesUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.Locale;

/**
 * @author Erik Andersson
 */
@Component(value = "unpublishFolderBackingBean")
@Scope(value = "request")
public class UnpublishFolderBackingBean {

    @Autowired
    private FacesUtil facesUtil;

    @Autowired
    private FolderService folderService;

    @Autowired
    private MessageSource messageSource;

    private List<JournalFolder> rootFolders;

    private long folderIdToUnpublish;

    public List<JournalFolder> getRootFolders() {
        return rootFolders;
    }

    public void setRootFolders(List<JournalFolder> rootFolders) {
        this.rootFolders = rootFolders;
    }

    public long getFolderIdToUnpublish() {
        return folderIdToUnpublish;
    }

    public void setFolderIdToUnpublish(long folderIdToUnpublish) {
        this.folderIdToUnpublish = folderIdToUnpublish;
    }

    public void unpublishFolder() {
        try {
            folderService.unpublishFolder(folderIdToUnpublish);
            addFacesMessage("unpublish-folder-success", FacesMessage.SEVERITY_INFO);
        } catch (UnpublishFolderException e) {
            addFacesMessage(e.getMessage(), FacesMessage.SEVERITY_ERROR);
        }
    }

    @PostConstruct
    public void init() {
        long scopeGroupId = facesUtil.getThemeDisplay().getScopeGroupId();
        rootFolders = folderService.getRootFolders(scopeGroupId);
    }

    public void addFacesMessage(String errorMessageKey, FacesMessage.Severity severity) {
        Locale locale = facesUtil.getLocale();

        String localizedMessage = messageSource.getMessage(errorMessageKey, new Object[0], locale);

        FacesMessage message = new FacesMessage(severity, localizedMessage, localizedMessage);
        FacesContext.getCurrentInstance().addMessage("", message);
    }


}
