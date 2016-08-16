package se.vgregion.reklistan.backingbean;


import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.journal.model.JournalFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.exception.DuplicateFolderNameException;
import se.vgregion.reklistan.service.FolderService;
import se.vgregion.reklistan.util.FacesUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.Locale;

/**
 * @author Erik Andersson
 */
@Component(value = "cloneFolderBackingBean")
@Scope(value = "request")
public class CloneFolderBackingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloneFolderBackingBean.class);

    @Autowired
    private FacesUtil facesUtil;

    @Autowired
    private FolderService folderService;

    @Autowired
    private MessageSource messageSource;

    private List<JournalFolder> rootFolders;

    private long folderIdToClone;

    private String folderNameNew;

    public List<JournalFolder> getRootFolders() {
        return rootFolders;
    }

    public void setRootFolders(List<JournalFolder> rootFolders) {
        this.rootFolders = rootFolders;
    }

    public long getFolderIdToClone() {
        return folderIdToClone;
    }

    public void setFolderIdToClone(long folderIdToClone) {
        this.folderIdToClone = folderIdToClone;
    }

    public String getFolderNameNew() {
        return folderNameNew;
    }

    public void setFolderNameNew(String folderNameNew) {
        this.folderNameNew = folderNameNew;
    }

    public String cloneFolder() {
        //LOGGER.info("cloneFolder()");
        //LOGGER.info("cloneFolder() - folderIdToClone is: " + folderIdToClone);

        LOGGER.info("cloneFolder() -  new folder name is: " + folderNameNew);



        try {

            folderService.cloneFolder(folderIdToClone, folderNameNew);

            addFacesMessage("clone-folder-success", FacesMessage.SEVERITY_INFO);

//            FacesMessage message =
//                    new FacesMessage(FacesMessage.SEVERITY_INFO,
//                            "Folder successfully cloned", "Folder successfully cloned");
//            FacesContext.getCurrentInstance()
//                    .addMessage("", message);

        } catch (Exception e) {

            if(e instanceof DuplicateFolderNameException) {
                addFacesMessage("clone-folder-error-duplicate-folder-map-name", FacesMessage.SEVERITY_ERROR);
            } else {
                e.printStackTrace();
            }
        }


        return null;
       //return "clone_folder?faces-redirect=true&includeViewParams=true";
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
