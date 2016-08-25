package se.vgregion.reklistan.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.*;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.journal.DuplicateFolderNameException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalFolderLocalServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import com.liferay.portlet.journal.model.JournalFolder;
import se.vgregion.reklistan.constants.RekListanConstants;
import se.vgregion.reklistan.exception.CloneFolderException;
import se.vgregion.reklistan.exception.PublishFolderException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Erik Andersson
 */
@SuppressWarnings("unchecked")
@Service
public class FolderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderService.class);


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

    public void cloneFolder(long copyFromFolderId, String folderNameNew) throws CloneFolderException {

        if(copyFromFolderId <= 0) {
            throw new CloneFolderException("clone-folder-error-must-choose-a-folder-to-clone-from");
        } else if(folderNameNew.equals("")) {
            throw new CloneFolderException("clone-folder-error-must-specify-new-folder-name");
        }

        try {
            JournalFolder copyFromFolder = JournalFolderLocalServiceUtil.fetchFolder(copyFromFolderId);

            long groupId = copyFromFolder.getGroupId();
            long parentFolderId = 0;
            String newFolderDescription = "";
            String newFolderName = folderNameNew;

            if(newFolderName.equals("")) {
                newFolderName = copyFromFolder.getName() + " (kopia)";
            }

            JournalFolder newFolder = createFolder(copyFromFolder.getUserId(), groupId, parentFolderId, newFolderName, newFolderDescription);

            copyFolder(copyFromFolderId, newFolder.getFolderId(), true);

        } catch (DuplicateFolderNameException e) {
            throw new CloneFolderException("clone-folder-error-duplicate-folder-map-name", e);
        } catch (Exception e) {
            e.printStackTrace();
        }


        //copyFolder(copyFromFolderId, 0);
    }

    public void unpublishOldAndPublishNew(long folderIdToUnpublish, long folderIdToPublish) throws PublishFolderException {

        if(folderIdToUnpublish <= 0) {
            throw new PublishFolderException("publish-folder-error-must-choose-a-folder-to-unpublish");
        } else if(folderIdToPublish <= 0) {
            throw new PublishFolderException("publish-folder-error-must-choose-a-folder-to-publish");
        } else if(folderIdToUnpublish == folderIdToPublish) {
            throw new PublishFolderException("publish-folder-error-folders-cannot-be-the-same");
        }

        try {
            unpublishFolder(folderIdToUnpublish);
            publishFolder(folderIdToPublish);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void publishFolder(long folderIdToPublish) throws Exception {
        try {
            JournalFolder folderToPublish = JournalFolderLocalServiceUtil.fetchFolder(folderIdToPublish);

            long groupId = folderToPublish.getGroupId();

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToPublish);

            for(JournalArticle article : articles) {
                // Add view permission Guest
                addViewPermissionGuest(article);

                // Add view permission User
                addViewPermissionUser(article);
            }

            // Publish subfolders recursively
            List<JournalFolder> subFolders = JournalFolderLocalServiceUtil.getFolders(groupId, folderIdToPublish);
            for(JournalFolder subFolder : subFolders) {
                publishFolder(subFolder.getFolderId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void unpublishFolder(long folderIdToUnpublish) throws Exception {
        try {
            JournalFolder folderToUnpublish = JournalFolderLocalServiceUtil.fetchFolder(folderIdToUnpublish);

            long groupId = folderToUnpublish.getGroupId();
            long companyId = folderToUnpublish.getCompanyId();

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToUnpublish);

            for(JournalArticle article : articles) {
                Role ownerRole = RoleLocalServiceUtil.fetchRole(companyId, RoleConstants.OWNER);

                // Remove all permissions on article
                deleteAllPermissions(article, ownerRole.getRoleId());

                // Add Reviewer-Secretary permissions
                addReviewerSecretaryPermissions(article);
            }

            // Unpublish subfolders recursively
            List<JournalFolder> subFolders = JournalFolderLocalServiceUtil.getFolders(groupId, folderIdToUnpublish);
            for(JournalFolder subFolder : subFolders) {
                unpublishFolder(subFolder.getFolderId());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyFolder(long copyFromFolderId, long copyToFolderId, boolean isRoot) {

        try {
            JournalFolder copyFromFolder = JournalFolderLocalServiceUtil.fetchFolder(copyFromFolderId);

            long groupId = copyFromFolder.getGroupId();
            String newFolderDescription = "";
            String newFolderName = copyFromFolder.getName();

            JournalFolder newFolder = null;

            if(isRoot) {
                newFolder = JournalFolderLocalServiceUtil.fetchFolder(copyToFolderId);
            } else {
                // Clone folder
                newFolder = createFolder(copyFromFolder.getUserId(), groupId, copyToFolderId, newFolderName, newFolderDescription);
            }

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, copyFromFolderId);

            //LOGGER.info("copyFolder - articles count is: " + articles.size());

            for(JournalArticle article : articles) {
                boolean isLatestVersion = JournalArticleLocalServiceUtil.isLatestVersion(article.getGroupId(), article.getArticleId(), article.getVersion());

                // Copy only latest article
                if(isLatestVersion) {
                    JournalArticle copiedArticle = JournalArticleLocalServiceUtil.copyArticle(article.getUserId(), article.getGroupId(), article.getArticleId(), "", true, article.getVersion());

                    // After copy, move to correct folder (i.e. the newly created folder)
                    JournalArticle movedArticle = JournalArticleLocalServiceUtil.moveArticle(copiedArticle.getGroupId(), copiedArticle.getArticleId(), newFolder.getFolderId());

                    Role ownerRole = RoleLocalServiceUtil.fetchRole(movedArticle.getCompanyId(), RoleConstants.OWNER);

                    // Remove all permissions on movedArticle
                    deleteAllPermissions(movedArticle, ownerRole.getRoleId());

                    // Add Reviewer permissions
                    addReviewerPermissions(movedArticle);

                    // Add Reviewer-Secretary permissions
                    addReviewerSecretaryPermissions(movedArticle);
                }
            }

            // Copy subfolders recursively
            List<JournalFolder> copyFromSubFolders = JournalFolderLocalServiceUtil.getFolders(groupId, copyFromFolderId);
            for(JournalFolder copyFromSubFolder : copyFromSubFolders) {
                copyFolder(copyFromSubFolder.getFolderId(), newFolder.getFolderId(), false);
            }


            //} catch (SystemException e) {
        } catch (SystemException | PortalException e) {
            e.printStackTrace();
        }

    }

    private void addReviewerPermissions(JournalArticle article) {

        Role reviewerRole = getReviewerRole(article);

        if(reviewerRole != null) {
            String[] actionsIds = new String[]{ActionKeys.VIEW};
            setArticlePermissions(article, reviewerRole, actionsIds);
        }
    }

    private void addReviewerSecretaryPermissions(JournalArticle article) {

        Role reviewerSecretaryRole = getReviewerSecretaryRole(article);

        if(reviewerSecretaryRole != null) {
            String[] actionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
            setArticlePermissions(article, reviewerSecretaryRole, actionsIds);
        }
    }

    private void addViewPermissionGuest(JournalArticle article) {
        try {
            Role guestRole = RoleLocalServiceUtil.fetchRole(article.getCompanyId(), RoleConstants.GUEST);

            addViewPermission(article, guestRole);
        } catch (SystemException e) {
            e.printStackTrace();
        }
    }

    private void addViewPermissionUser(JournalArticle article) {
        try {
            Role userRole = RoleLocalServiceUtil.fetchRole(article.getCompanyId(), RoleConstants.USER);

            addViewPermission(article, userRole);
        } catch (SystemException e) {
            e.printStackTrace();
        }
    }

    private void addViewPermission(JournalArticle article, Role role) {
        String[] actionsIds = new String[]{ActionKeys.VIEW};
        setArticlePermissions(article, role, actionsIds);
    }

    private JournalFolder createFolder(long userId, long groupId, long parentFolderId, String folderName, String folderDescription) throws PortalException {

        JournalFolder newFolder = null;

        ServiceContext serviceContext = new ServiceContext();

        try {
            newFolder = JournalFolderLocalServiceUtil.addFolder(userId, groupId, parentFolderId, folderName, folderDescription , serviceContext);
        } catch (SystemException e) {
            e.printStackTrace();
        }

        return newFolder;
    }

    private void deleteAllPermissions(JournalArticle article, long ownerRoleId) {
        //LOGGER.info("deleteAllPermissions() - for artice: " + article.getTitle("sv_SE", true));

        try {

            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil.getResourceResourcePermissions(
                    article.getCompanyId(), article.getGroupId(), JournalArticle.class.getName(),
                    Long.toString(article.getResourcePrimKey())
            );


            for(ResourcePermission resourcePermission : resourcePermissions) {

                boolean isOwnerResource = resourcePermission.getRoleId() == ownerRoleId;

                // Owner Resource Permission cannot be deleted programatically. Fails silently. No permissions are removed if owner permissions are tried to be removed.

                if(!isOwnerResource) {
                    ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
                    //LOGGER.info("ResourcePermission was NOT owner permission. ResourcePermission was deleted.");
                } else {
                    //LOGGER.info("ResourcePermission WAS owner permission. ResourcePermission was kept.");
                }

            }
        } catch (SystemException e) {
            throw new RuntimeException("delete resource permission failed for article with id" + article.getId(), e);
        }
    }


    private String getReviewerRoleName(JournalArticle article) {

        String reviewerRoleName = "";

        try {
            reviewerRoleName = ExpandoValueLocalServiceUtil.getData(
                    article.getCompanyId(), JournalArticle.class.getName(),
                    ExpandoTableConstants.DEFAULT_TABLE_NAME, RekListanConstants.REVIEWER_ROLE_CUSTOM_FIELD,
                    article.getId(), "");

        } catch (SystemException | PortalException e) {
            throw new RuntimeException("getReviewerRoleName failed for article with id" + article.getId(), e);
        }

        return reviewerRoleName;
    }

    private Role getReviewerRole(JournalArticle article) {
        Role reviewerRole = null;

        String reviewerRoleName = getReviewerRoleName(article);

        try {
            reviewerRole = RoleLocalServiceUtil.fetchRole(article.getCompanyId(), reviewerRoleName);
        } catch (SystemException e) {
            throw new RuntimeException("getReviewerRole failed for article with id" + article.getId(), e);
        }

        return reviewerRole;
    }

    private Role getReviewerSecretaryRole(JournalArticle article) {
        Role reviewerSecretaryRole = null;

        String reviewerRoleName = getReviewerRoleName(article);
        String reviewerSecretaryRoleName = reviewerRoleName + RekListanConstants.REVIEWER_ROLE_SECRETARY_SUFFIX;

        try {
            reviewerSecretaryRole = RoleLocalServiceUtil.fetchRole(article.getCompanyId(), reviewerSecretaryRoleName);
        } catch (SystemException e) {
            throw new RuntimeException("getReviewerRole failed for article with id" + article.getId(), e);
        }

        return reviewerSecretaryRole;
    }

    private void setArticlePermissions(JournalArticle article, Role role, String[] actionIds) {
        try {
            ResourcePermissionLocalServiceUtil.setResourcePermissions(
                    article.getCompanyId(), JournalArticle.class.getName(),
                    ResourceConstants.SCOPE_INDIVIDUAL, Long.toString(article.getResourcePrimKey()),
                    role.getRoleId(), actionIds);
        } catch (SystemException | PortalException e) {
            throw new RuntimeException("addReviewerPermissions failed for article with id" + article.getId(), e);
        }
    }


}