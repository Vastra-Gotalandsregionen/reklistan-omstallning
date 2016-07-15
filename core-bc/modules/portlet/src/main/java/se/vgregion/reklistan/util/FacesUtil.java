package se.vgregion.reklistan.util;

import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.portlet.PortletRequest;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class FacesUtil {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FacesUtil.class);

    public ThemeDisplay getThemeDisplay() {
        ExternalContext externalContext =
                FacesContext.getCurrentInstance().getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();

        return (ThemeDisplay)requestMap.get(WebKeys.THEME_DISPLAY);
    }

    public PortletRequest getRequest() {
        ExternalContext externalContext =
                FacesContext.getCurrentInstance().getExternalContext();

        return (PortletRequest)externalContext.getRequest();
    }

    public Long fetchId(String parameterName) {

        String value = fetchProperty(parameterName);

        Long id = null;

        if(value != null) {
            id = Long.parseLong(value);
        }

        return id;
    }

    public String fetchProperty(String parameterName) {

        ExternalContext externalContext =
                FacesContext.getCurrentInstance().getExternalContext();

        Map<String, String> parameterMap =
                externalContext.getRequestParameterMap();

        String value = parameterMap.get(parameterName);

        LOGGER.info("FacesUtil - fetchProperty(): {}", value);

        return value;
    }

    public boolean fetchBooleanProperty(String parameterName) {

        String value = fetchProperty(parameterName);

        boolean valueBoolean = Boolean.parseBoolean(value);

        LOGGER.info("FacesUtil - fetchBooleanProperty(): {}", valueBoolean);

        return valueBoolean;
    }

}
