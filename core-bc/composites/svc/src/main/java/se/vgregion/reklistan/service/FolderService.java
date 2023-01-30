package se.vgregion.reklistan.service;

import com.liferay.document.library.kernel.exception.DuplicateFolderNameException;
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
import com.liferay.portal.kernel.util.StringPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import se.vgregion.reklistan.constants.AldrekompassenConstants;
import se.vgregion.reklistan.exception.CloneFolderException;
import se.vgregion.reklistan.exception.PublishFolderException;

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
            Role readRole = getRoleByName(companyId, AldrekompassenConstants.READ_ROLE_NAME);
            Role writeRole = getRoleByName(companyId, AldrekompassenConstants.WRITE_ROLE_NAME);
            Role publishRole = getRoleByName(companyId, AldrekompassenConstants.PUBLISH_ROLE_NAME);

            // Delete all folder permissions
            deleteAllFolderPermissions(folderToPublish, ownerRole.getRoleId());

            // Set view folder permissions
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToPublish, ownerRole, folderActionsIds);
            setFolderPermissions(folderToPublish, readRole, folderActionsIds);
            setFolderPermissions(folderToPublish, writeRole, folderActionsIds);
            setFolderPermissions(folderToPublish, publishRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToPublish);

            for(JournalArticle article : articles) {

                // Remove all permissions on article
                deleteAllArticlePermissions(article, ownerRole.getRoleId());

                // Add view permissions
                String[] viewActionIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, guestRole, viewActionIds);
                setArticlePermissions(article, readRole, viewActionIds);
                setArticlePermissions(article, writeRole, viewActionIds);

                // Add view and update permissions (only publisher can make updates to published articles
                String[] updateActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
                setArticlePermissions(article, publishRole, updateActionsIds);
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
            Role guestRole = getRoleByName(companyId, RoleConstants.GUEST);
            Role readRole = getRoleByName(companyId, AldrekompassenConstants.READ_ROLE_NAME);
            Role writeRole = getRoleByName(companyId, AldrekompassenConstants.WRITE_ROLE_NAME);
            Role publishRole = getRoleByName(companyId, AldrekompassenConstants.PUBLISH_ROLE_NAME);

            // Delete all folder permissions
            deleteAllFolderPermissions(folderToUnpublish, ownerRole.getRoleId());

            // Set view folder permissions
            String[] folderActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(folderToUnpublish, ownerRole, folderActionsIds);
            setFolderPermissions(folderToUnpublish, readRole, folderActionsIds);
            setFolderPermissions(folderToUnpublish, writeRole, folderActionsIds);
            setFolderPermissions(folderToUnpublish, publishRole, folderActionsIds);

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, folderIdToUnpublish);

            for(JournalArticle article : articles) {
                // Remove all permissions on article
                deleteAllArticlePermissions(article, ownerRole.getRoleId());

                // Add view permissions
                String[] lkActionsIds = new String[]{ActionKeys.VIEW};
                setArticlePermissions(article, readRole, lkActionsIds);
                setArticlePermissions(article, writeRole, lkActionsIds);
                setArticlePermissions(article, publishRole, lkActionsIds);
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
            Role readRole = getRoleByName(companyId, AldrekompassenConstants.READ_ROLE_NAME);
            Role writeRole = getRoleByName(companyId, AldrekompassenConstants.WRITE_ROLE_NAME);
            Role publishRole = getRoleByName(companyId, AldrekompassenConstants.PUBLISH_ROLE_NAME);

            if(isRoot) {
                newFolder = JournalFolderLocalServiceUtil.fetchFolder(copyToFolderId);
            } else {
                // Clone folder
                newFolder = createFolder(copyFromFolder.getUserId(), groupId, copyToFolderId, copyFromFolder.getName(), newFolderDescription);
            }

            // Delete all folder permissions
            deleteAllFolderPermissions(newFolder, ownerRole.getRoleId());

            // Set folder permissions to view for the application specific roles
            String[] folderViewActionsIds = new String[]{ActionKeys.VIEW};
            setFolderPermissions(newFolder, readRole, folderViewActionsIds);

            // TODO It's questionable whether we should allow users to create subfolders since the permissions would then be arbitrary.
//            String[] folderAddFolderActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.ADD_SUBFOLDER, ActionKeys.DELETE};
            setFolderPermissions(newFolder, writeRole, folderViewActionsIds);
            setFolderPermissions(newFolder, publishRole, folderViewActionsIds);

            if (!isRoot) {
                // If it's a subfolder the write and publish role should be able to add articles. This will overwrite
                // permissions for write and publish role set just before.
                String[] actionIds = {ActionKeys.VIEW, ActionKeys.ADD_ARTICLE};
                setFolderPermissions(newFolder, writeRole, actionIds);
                setFolderPermissions(newFolder, publishRole, actionIds);
            }

            // Get articles
            List<JournalArticle> articles = JournalArticleLocalServiceUtil.getArticles(groupId, copyFromFolderId);

            for(JournalArticle article : articles) {
                boolean isLatestVersion = JournalArticleLocalServiceUtil.isLatestVersion(article.getGroupId(), article.getArticleId(), article.getVersion());

                //JournalArticleLocalServiceUtil.getLatestArticle(long groupId, String articleId, int status)

                // Copy only latest article
                if(isLatestVersion) {
                    JournalArticle copiedArticle = JournalArticleLocalServiceUtil.copyArticle(article.getUserId(), article.getGroupId(), article.getArticleId(), "", true, article.getVersion());

                    updateTitleMap(serviceContext, copiedArticle);

                    // After copy, move to correct folder (i.e. the newly created folder)
                    JournalArticle movedArticle = JournalArticleLocalServiceUtil.moveArticle(copiedArticle.getGroupId(), copiedArticle.getArticleId(), newFolder.getFolderId());

                    // Remove all permissions on movedArticle
                    deleteAllArticlePermissions(movedArticle, ownerRole.getRoleId());

                    // Add view permissions
                    String[] viewActionsIds = new String[]{ActionKeys.VIEW};
                    setArticlePermissions(movedArticle, guestRole, viewActionsIds);
                    setArticlePermissions(movedArticle, readRole, viewActionsIds);

                    // Add write permissions
                    String[] writeActionsIds = new String[]{ActionKeys.VIEW, ActionKeys.UPDATE};
                    setArticlePermissions(movedArticle, writeRole, writeActionsIds);
                    setArticlePermissions(movedArticle, publishRole, writeActionsIds);

                    continue;
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
            String duplicateSuffix = StringPool.SPACE + LanguageUtil.get(localeTitleEntry.getKey(), "duplicate");
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
