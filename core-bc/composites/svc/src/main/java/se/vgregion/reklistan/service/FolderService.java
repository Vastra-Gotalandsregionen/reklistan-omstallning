package se.vgregion.reklistan.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.*;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
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

            JournalFolder newFolder = createFolder(copyFromFolder.getUserId(), groupId, parentFolderId, newFolderName, newFolderDescription);

            copyFolder(copyFromFolderId, newFolder.getFolderId(), true);

        } catch (DuplicateFolderNameException e) {
            throw new CloneFolderException("clone-folder-error-duplicate-folder-map-name", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            long companyId = folderToPublish.getCompanyId();

            // Roles common to all articles
            Role ownerRole = getRoleByName(companyId, RoleConstants.OWNER);
            Role guestRole = getRoleByName(companyId, RoleConstants.GUEST);
            Role userRole = getRoleByName(companyId, RoleConstants.USER);
            Role lkSecretaryRole = getRoleByName(companyId, RekListanConstants.LK_SECRETARY_ROLE_NAME);

            // Delete all folder permissions
            deleteAllFolderPermissions(folderToPublish, ownerRole.getRoleId());

            // Set folder permissions to view for User and guestrole
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToPublish, userRole, folderActionsIds);
            setFolderPermissions(folderToPublish, guestRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToPublish);

            for(JournalArticle article : articles) {

                // Remove all permissions on article
                deleteAllArticlePermissions(article, ownerRole.getRoleId());

                // Add Guest permissions (VIEW)
                String[] guestActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, guestRole, guestActionsIds);

                // Add User permissions (VIEW)
                String[] userActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, userRole, userActionsIds);


                // Add LK Secretary permissions (VIEW and UPDATE)
                String[] lkSecretaryActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
                setArticlePermissions(article, lkSecretaryRole, lkSecretaryActionsIds);
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

            // Roles common to all articles
            Role ownerRole = getRoleByName(companyId, RoleConstants.OWNER);
            Role lkRole = getRoleByName(companyId, RekListanConstants.LK_ROLE_NAME);
            Role lkSecretaryRole = getRoleByName(companyId, RekListanConstants.LK_SECRETARY_ROLE_NAME);

            // Delete all folder permissions
            deleteAllFolderPermissions(folderToUnpublish, ownerRole.getRoleId());

            // Set folder permissions to view for LK and LK secretary
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToUnpublish, lkRole, folderActionsIds);
            setFolderPermissions(folderToUnpublish, lkSecretaryRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToUnpublish);

            for(JournalArticle article : articles) {
                // Remove all permissions on article
                deleteAllArticlePermissions(article, ownerRole.getRoleId());

                // Add LK permissions (VIEW)
                String[] lkActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, lkRole, lkActionsIds);

                // Add LK Secretary permissions (VIEW)
                String[] lkSecretaryActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, lkSecretaryRole, lkSecretaryActionsIds);
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
            long companyId = copyFromFolder.getCompanyId();
            String newFolderDescription = "";
            String newFolderName = copyFromFolder.getName();

            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setScopeGroupId(groupId);

            JournalFolder newFolder = null;

            // Roles common to all articles
            Role ownerRole = getRoleByName(companyId, RoleConstants.OWNER);
            Role guestRole = getRoleByName(companyId, RoleConstants.GUEST);
            Role userRole = getRoleByName(companyId, RoleConstants.USER);
            Role lkRole = getRoleByName(companyId, RekListanConstants.LK_ROLE_NAME);
            Role lkSecretaryRole = getRoleByName(companyId, RekListanConstants.LK_SECRETARY_ROLE_NAME);

            if(isRoot) {
                newFolder = JournalFolderLocalServiceUtil.fetchFolder(copyToFolderId);
            } else {
                // Clone folder
                newFolder = createFolder(copyFromFolder.getUserId(), groupId, copyToFolderId, newFolderName, newFolderDescription);
            }

            // Delete all folder permissions
            deleteAllFolderPermissions(newFolder, ownerRole.getRoleId());

            // Set folder permissions to view for User and guestrole
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(newFolder, userRole, folderActionsIds);
            setFolderPermissions(newFolder, guestRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, copyFromFolderId);

            for(JournalArticle article : articles) {
                boolean isLatestVersion = JournalArticleLocalServiceUtil.isLatestVersion(article.getGroupId(), article.getArticleId(), article.getVersion());

                //JournalArticleLocalServiceUtil.getLatestArticle(long groupId, String articleId, int status)

                // Copy only latest article
                if(isLatestVersion) {
                    //article.getStatus()
                    //WorkflowConstants.STATUS_APPROVED
                    boolean isLatestApprovedVersion = article.getStatus() == WorkflowConstants.STATUS_APPROVED;

                    LOGGER.info("copiedArticle isLatestApprovedVersion: " + isLatestApprovedVersion + " with name: " + article.getTitle("sv_SE"));


                    JournalArticle copiedArticle = JournalArticleLocalServiceUtil.copyArticle(article.getUserId(), article.getGroupId(), article.getArticleId(), "", true, article.getVersion());

                    //LOGGER.info("copiedArticle version: " + article.getVersion() + " with name: " + copiedArticle.getTitle("sv_SE"));

                    // After copy, move to correct folder (i.e. the newly created folder)
                    JournalArticle movedArticle = JournalArticleLocalServiceUtil.moveArticle(copiedArticle.getGroupId(), copiedArticle.getArticleId(), newFolder.getFolderId());

                    // Reviewer Role (article specific)
                    String reviewerRoleName = getReviewerRoleName(movedArticle);
                    Role reviewerRole = getRoleByName(companyId, reviewerRoleName);

                    // Reviewer Secretary Role (article specific)
                    String reviewerSecretaryRoleName = reviewerRoleName + RekListanConstants.REVIEWER_ROLE_SECRETARY_SUFFIX;
                    Role reviewerSecretaryRole = getRoleByName(companyId, reviewerSecretaryRoleName);

                    // Remove all permissions on movedArticle
                    deleteAllArticlePermissions(movedArticle, ownerRole.getRoleId());

                    // Add Reviewer permissions (VIEW)
                    String[] reviwerActionsIds = new String[]{ActionKeys.VIEW};
                    setArticlePermissions(movedArticle, reviewerRole, reviwerActionsIds);

                    // Add Reviewer-Secretary permissions (VIEW and UPDATE)
                    String[] reviewerSecretaryActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
                    setArticlePermissions(movedArticle, reviewerSecretaryRole, reviewerSecretaryActionsIds);

                    // Add LK permissions (VIEW)
                    String[] lkActionsIds = new String[]{ActionKeys.VIEW};
                    setArticlePermissions(movedArticle, lkRole, lkActionsIds);

                    // Add LK Secretary permissions (VIEW and UPDATE)
                    String[] lkSecretaryActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
                    setArticlePermissions(movedArticle, lkSecretaryRole, lkSecretaryActionsIds);
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

    private void deleteAllArticlePermissions(JournalArticle article, long ownerRoleId) {
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
                }
            }
        } catch (SystemException e) {
            throw new RuntimeException("delete resource permission failed for article with id" + article.getId(), e);
        }
    }

    private void deleteAllFolderPermissions(JournalFolder folder, long ownerRoleId) {
        try {
            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil.getResourceResourcePermissions(
                    folder.getCompanyId(), folder.getGroupId(), JournalFolder.class.getName(),
                    Long.toString(folder.getFolderId())
            );

            for(ResourcePermission resourcePermission : resourcePermissions) {
                boolean isOwnerResource = resourcePermission.getRoleId() == ownerRoleId;

                // Owner Resource Permission cannot be deleted programatically. Fails silently. No permissions are removed if owner permissions are tried to be removed.

                if(!isOwnerResource) {
                    ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
                }
            }
        } catch (SystemException e) {
            throw new RuntimeException("delete resource permission failed for folder with id" + folder.getFolderId(), e);
        }
    }


    private Role getRoleByName(long companyId, String roleName) {
        Role role = null;

        try {
            role = RoleLocalServiceUtil.fetchRole(companyId, roleName);
        } catch (SystemException e) {
            throw new RuntimeException("getRoleByName failed for role name: " + roleName, e);
        }

        return role;
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

    private void setArticlePermissions(JournalArticle article, Role role, String[] actionIds) {
        if(role != null) {
            try {
                ResourcePermissionLocalServiceUtil.setResourcePermissions(
                        article.getCompanyId(), JournalArticle.class.getName(),
                        ResourceConstants.SCOPE_INDIVIDUAL, Long.toString(article.getResourcePrimKey()),
                        role.getRoleId(), actionIds);
            } catch (SystemException | PortalException e) {
                throw new RuntimeException("setArticlePermissions failed for article with id" + article.getId(), e);
            }
        }
    }

    private void setFolderPermissions(JournalFolder folder, Role role, String[] actionIds) {
        if(role != null) {
            try {
                ResourcePermissionLocalServiceUtil.setResourcePermissions(
                        folder.getCompanyId(), JournalFolder.class.getName(),
                        ResourceConstants.SCOPE_INDIVIDUAL, Long.toString(folder.getFolderId()),
                        role.getRoleId(), actionIds);
            } catch (SystemException | PortalException e) {
                throw new RuntimeException("setFolderPermissions failed for folder with id" + folder.getFolderId(), e);
            }
        }
    }



}