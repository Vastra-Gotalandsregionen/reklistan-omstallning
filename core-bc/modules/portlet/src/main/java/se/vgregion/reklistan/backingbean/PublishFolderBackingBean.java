package se.vgregion.reklistan.backingbean;


import com.liferay.journal.model.JournalFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.exception.PublishFolderException;
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
@Component(value = "publishFolderBackingBean")
@Scope(value = "request")
public class PublishFolderBackingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishFolderBackingBean.class);

    @Autowired
    private FacesUtil facesUtil;

    @Autowired
    private FolderService folderService;

    @Autowired
    private MessageSource messageSource;

    private String portletNamespace;

    public String getPortletNamespace() {
        return portletNamespace;
    }

    public void setPortletNamespace(String portletNamespace) {
        this.portletNamespace = portletNamespace;
    }

    private List<JournalFolder> rootFolders;

    private long folderIdToPublish;

    private long folderIdToUnpublish;

    public List<JournalFolder> getRootFolders() {
        return rootFolders;
    }

    public void setRootFolders(List<JournalFolder> rootFolders) {
        this.rootFolders = rootFolders;
    }

    public long getFolderIdToPublish() {
        return folderIdToPublish;
    }

    public void setFolderIdToPublish(long folderIdToPublish) {
        this.folderIdToPublish = folderIdToPublish;
    }

    public long getFolderIdToUnpublish() {
        return folderIdToUnpublish;
    }

    public void setFolderIdToUnpublish(long folderIdToUnpublish) {
        this.folderIdToUnpublish = folderIdToUnpublish;
    }

    public String publishFolder() {
        String returnView = null;


        try {
            folderService.unpublishOldAndPublishNew(folderIdToUnpublish, folderIdToPublish);

            addFacesMessage("publish-folder-success", FacesMessage.SEVERITY_INFO);
        } catch (PublishFolderException e) {
            addFacesMessage(e.getMessage(), FacesMessage.SEVERITY_ERROR);
        }
        return returnView;
    }

    @PostConstruct
    public void init() {
        portletNamespace = FacesContext.getCurrentInstance().getExternalContext().encodeNamespace("");
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
