package se.vgregion.reklistan.service;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.journal.service.JournalFolderLocalServiceUtil;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import com.liferay.portlet.journal.model.JournalFolder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Erik Andersson
 */
@SuppressWarnings("unchecked")
@Service
public class FolderService {


    @PostConstruct
    public void init() {

    }

    public String getTestString() {
        return "Test String from Service";
    }

    public List<JournalFolder> getRootFolders(long groupId) {
        int parentFolderId= 0;
        List<JournalFolder> folders = new ArrayList<JournalFolder>();

        try {
            folders = JournalFolderLocalServiceUtil.getFolders(groupId, parentFolderId);
        } catch (SystemException e) {
            e.printStackTrace();
        }

        return folders;

    }


}
