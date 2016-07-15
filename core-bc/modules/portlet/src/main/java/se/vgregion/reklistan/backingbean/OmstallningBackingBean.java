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

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;

/**
 * @author Erik Andersson
 */
@Component(value = "omstallningBackingBean")
@ViewScoped
public class OmstallningBackingBean  {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmstallningBackingBean.class);

    @Autowired
    private FolderService folderService;

    @PostConstruct
    public void init() {
    }

    public String getTestString() {
        return "test";
    }

    public String getTestStringFromService() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        ThemeDisplay themeDisplay = (ThemeDisplay)externalContext.getRequestMap().get(WebKeys.THEME_DISPLAY);
        return folderService.getTestString();
    }

}