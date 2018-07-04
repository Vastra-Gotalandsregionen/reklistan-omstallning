package se.vgregion.reklistan.backingbean;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.vgregion.reklistan.service.FolderService;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

/**
 * @author Erik Andersson
 */
@Component(value = "omstallningBackingBean")
@ViewScoped
public class OmstallningBackingBean  {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmstallningBackingBean.class);

    @Autowired
    private FolderService folderService;

    private String portletNamespace;

    public String getPortletNamespace() {
        return portletNamespace;
    }

    public void setPortletNamespace(String portletNamespace) {
        this.portletNamespace = portletNamespace;
    }

    @PostConstruct
    public void init() {
        portletNamespace = FacesContext.getCurrentInstance().getExternalContext().encodeNamespace("");
    }

}
