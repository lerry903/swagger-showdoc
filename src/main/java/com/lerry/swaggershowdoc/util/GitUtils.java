package com.lerry.swaggershowdoc.util;

import org.apache.commons.codec.binary.Base64;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.RepositoryFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GitUtils {


    private static GitLabApi gitLabApi;

    @Autowired
    public void setGitLabApi(GitLabApi gitLabApi){
        GitUtils.gitLabApi = gitLabApi;
    }

    /**
     * 从Git中下载文件
     * @param filePath
     * @param projectName
     * @param ref
     * @return
     */
    public static String downLoadFile(String filePath, String projectName, String ref){
        RepositoryFile file;
        byte[] bytes;
        try {
            Integer projectId = getProjectId(projectName);
            file = gitLabApi.getRepositoryFileApi().getFile(filePath, projectId, ref);
            String content = file.getContent();
            bytes = Base64.decodeBase64(content.getBytes());
        return new String(bytes,"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取项目id
     * @param projectName
     * @return
     */
    private static Integer getProjectId(String projectName){
        try {
            List<Project> projects = gitLabApi.getProjectApi().getProjects(projectName);
            if(projects.size() == 1){
                return projects.get(0).getId();
            }
        } catch (GitLabApiException e) {
            e.printStackTrace();
        }
        return null;
    }

}
