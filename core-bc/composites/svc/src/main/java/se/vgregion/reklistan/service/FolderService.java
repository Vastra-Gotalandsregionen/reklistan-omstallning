package se.vgregion.reklistan.service;

import com.liferay.document.library.kernel.exception.DuplicateFolderNameException;
import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.journal.service.JournalFolderLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import se.vgregion.reklistan.constants.RekListanConstants;
import se.vgregion.reklistan.exception.CloneFolderException;
import se.vgregion.reklistan.exception.PublishFolderException;
import se.vgregion.reklistan.exception.UnpublishFolderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            String externalReferenceCode = EXTERNAL_REFERENCE_CODE_PREFIX + folderNameNew;
            boolean isRoot = true;

            JournalFolder newFolder = createFolder(externalReferenceCode, copyFromFolder.getUserId(), groupId,
                    parentFolderId, newFolderName, newFolderDescription);

            copyFolder(copyFromFolderId, newFolder.getFolderId(), isRoot);

        } catch (DuplicateFolderNameException e) {
            throw new CloneFolderException("clone-folder-error-duplicate-folder-map-name", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishFolder(long folderIdToPublish) throws PublishFolderException {

        if(folderIdToPublish <= 0) {
            throw new PublishFolderException("publish-folder-error-must-choose-a-folder-to-publish");
        }

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

    public void unpublishFolder(long folderIdToUnpublish) throws UnpublishFolderException {

        if(folderIdToUnpublish <= 0) {
            throw new UnpublishFolderException("unpublish-folder-error-must-choose-a-folder-to-unpublish");
        }

        try {
            JournalFolder folderToUnpublish = JournalFolderLocalServiceUtil.fetchFolder(folderIdToUnpublish);

            long groupId = folderToUnpublish.getGroupId();
            long companyId = folderToUnpublish.getCompanyId();

            // Roles common to all articles
            Role ownerRole = getRoleByName(companyId, RoleConstants.OWNER);
            Role userRole = getRoleByName(companyId, RoleConstants.USER);
            Role lkRole = getRoleByName(companyId, RekListanConstants.LK_ROLE_NAME);
            Role lkSecretaryRole = getRoleByName(companyId, RekListanConstants.LK_SECRETARY_ROLE_NAME);

            // Delete all folder permissions
            deleteAllFolderPermissions(folderToUnpublish, ownerRole.getRoleId());

            // Set folder permissions to view for LK and LK secretary
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToUnpublish, lkRole, folderActionsIds);
            setFolderPermissions(folderToUnpublish, lkSecretaryRole, folderActionsIds);

            // Set folder permissions to view for USER
            String[] userFolderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToUnpublish, userRole, userFolderActionsIds);


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

                // Add User permissions (VIEW)
                String[] userActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, userRole, userActionsIds);
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
                String newFolderName = copyFromFolder.getName();
                String externalReferenceCode = EXTERNAL_REFERENCE_CODE_PREFIX + newFolderName;

                newFolder = createFolder(externalReferenceCode, copyFromFolder.getUserId(), groupId, copyToFolderId,
                        newFolderName, newFolderDescription);
            }

            // Delete all folder permissions
            deleteAllFolderPermissions(newFolder, ownerRole.getRoleId());

            // Set folder permissions to view for User
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(newFolder, userRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, copyFromFolderId);

            for(JournalArticle article : articles) {
                boolean isLatestVersion = JournalArticleLocalServiceUtil.isLatestVersion(article.getGroupId(), article.getArticleId(), article.getVersion());

                // Copy only latest article
                if(isLatestVersion) {
                    JournalArticle copiedArticle = JournalArticleLocalServiceUtil.copyArticle(article.getUserId(), article.getGroupId(), article.getArticleId(), "", true, article.getVersion());

                    updateTitleMap(serviceContext, copiedArticle);

                    // After copy, move to correct folder (i.e. the newly created folder)
                    JournalArticle movedArticle = JournalArticleLocalServiceUtil.moveArticle(copiedArticle.getGroupId(),
                            copiedArticle.getArticleId(), newFolder.getFolderId(), serviceContext);

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

    private void updateTitleMap(ServiceContext serviceContext, JournalArticle copiedArticle) throws PortalException {
        Map<Locale, String> articleTitleMap = copiedArticle.getTitleMap();

        Map<Locale, String> titleMapWithoutDuplicateSuffix = new HashMap<>();

        for (Map.Entry<Locale, String> localeTitleEntry : articleTitleMap.entrySet()) {
            String duplicateSuffix = " " + LanguageUtil.get(localeTitleEntry.getKey(), "duplicate");
            String[] split = localeTitleEntry.getValue().split(duplicateSuffix);
            titleMapWithoutDuplicateSuffix.put(localeTitleEntry.getKey(), split[0]);
        }

        copiedArticle.setTitleMap(titleMapWithoutDuplicateSuffix);

        JournalArticleLocalServiceUtil.updateArticle(
                copiedArticle.getUserId(),
                copiedArticle.getGroupId(),
                copiedArticle.getFolderId(),
                copiedArticle.getArticleId(),
                copiedArticle.getVersion(),
                titleMapWithoutDuplicateSuffix,
                copiedArticle.getDescriptionMap(),
                copiedArticle.getContent(),
                copiedArticle.getLayoutUuid(),
                serviceContext);
    }

    private JournalFolder createFolder(String externalReferenceCode, long userId, long groupId, long parentFolderId, String folderName, String folderDescription) throws PortalException {

        JournalFolder newFolder = null;

        ServiceContext serviceContext = new ServiceContext();

        try {
            newFolder = JournalFolderLocalServiceUtil.addFolder(externalReferenceCode, userId, groupId, parentFolderId,
                    folderName, folderDescription , serviceContext);
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

    static String EXTERNAL_REFERENCE_CODE_PREFIX = "rek-";

}
