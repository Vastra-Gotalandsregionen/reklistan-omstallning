package se.vgregion.reklistan.backingbean;


import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.journal.model.JournalFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.service.FolderService;
import se.vgregion.reklistan.util.FacesUtil;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;

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

    private List<JournalFolder> rootFolders;

    private long folderIdToClone;

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

    public String cloneFolder() {
        LOGGER.info("cloneFolder()");
        LOGGER.info("cloneFolder() - folderIdToClone is: " + folderIdToClone);

        folderService.cloneFolder(folderIdToClone);

        return "clone_folder";
    }

    @PostConstruct
    public void init() {
        long scopeGroupId = facesUtil.getThemeDisplay().getScopeGroupId();
        rootFolders = folderService.getRootFolders(scopeGroupId);
    }


}
