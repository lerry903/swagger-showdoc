package com.lerry.swaggershowdoc.config;

import org.gitlab4j.api.GitLabApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitlabConfig {

    @Bean
    public GitLabApi gitLabApi (){
        return new GitLabApi("http://121.40.242.195", "Hgyedp4XZYV7Bppx3sR3");
    }

}
