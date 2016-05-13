package se.vgregion.reklistan.backingbean;


import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.journal.model.JournalFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.service.FolderService;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;

/**
 * @author Erik Andersson
 */
@Component(value = "cloneFolderBackingBean")
@ViewScoped
public class CloneFolderBackingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloneFolderBackingBean.class);

    @Autowired
    private FolderService folderService;

    private List<JournalFolder> rootFolders;

    @PostConstruct
    public void init() {
        rootFolders = fetchRootFolders();
    }

    public List<JournalFolder> fetchRootFolders() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        ThemeDisplay themeDisplay = (ThemeDisplay)externalContext.getRequestMap().get(WebKeys.THEME_DISPLAY);
        long scopeGroupId = themeDisplay.getScopeGroupId();

        return folderService.getRootFolders(scopeGroupId);
    }

    public List<JournalFolder> getRootFolders() {
        return rootFolders;
    }

    public void setRootFolders(List<JournalFolder> rootFolders) {
        this.rootFolders = rootFolders;
    }

}
