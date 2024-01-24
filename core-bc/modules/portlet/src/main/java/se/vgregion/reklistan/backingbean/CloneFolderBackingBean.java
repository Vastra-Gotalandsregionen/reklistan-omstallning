package se.vgregion.reklistan.backingbean;


import com.liferay.journal.model.JournalFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.exception.CloneFolderException;
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
@Component(value = "cloneFolderBackingBean")
@Scope(value = "request")
public class CloneFolderBackingBean {

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

    private String foo;

    public String getFoo() { return foo; }

    public void setFoo(String foo) { this.foo = foo; }

    public String getFolderNameNew() {
        return folderNameNew;
    }

    public void setFolderNameNew(String folderNameNew) {
        this.folderNameNew = folderNameNew;
    }

    public void cloneFolder() {
        try {
            folderService.cloneFolder(folderIdToClone, folderNameNew);

            addFacesMessage("clone-folder-success", FacesMessage.SEVERITY_INFO);
        } catch (CloneFolderException e) {
            addFacesMessage(e.getMessage(), FacesMessage.SEVERITY_ERROR);
        }
    }

    @PostConstruct
    public void init() {
        foo = "Foo";

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
