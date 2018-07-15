package com.lerry.swaggershowdoc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lerry.swaggershowdoc.swagger.XforcceSwagger2MarkupConverter;
import io.github.swagger2markup.Language;
import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class SwaggerUtils {


    private static OkHttpUtil okHttpUtil;

    @Autowired
    public void setOkHttpUtil(OkHttpUtil okHttpUtil) {
        SwaggerUtils.okHttpUtil = okHttpUtil;
    }

    private static Swagger2MarkupConfig config;

    static {
        config = new Swagger2MarkupConfigBuilder()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .withOutputLanguage(Language.ZH)
                .withFlatBody()
                .withGeneratedExamples()
                .build();
    }

    private static String getExternalDocs(Path path){
        if(null!= path.getPost()){
            return path.getPost().getTags().get(0)+"/"+path.getPost().getOperationId();
        }
        if(null!= path.getGet()){
            return path.getGet().getTags().get(0)+"/"+path.getGet().getOperationId();
        }
        if(null!= path.getHead()){
            return path.getHead().getTags().get(0)+"/"+path.getHead().getOperationId();
        }
        if(null!= path.getDelete()){
            return path.getDelete().getTags().get(0)+"/"+path.getDelete().getOperationId();
        }
        if(null!= path.getOptions()){
            return path.getOptions().getTags().get(0)+"/"+path.getOptions().getOperationId();
        }
        if(null!= path.getPut()){
            return path.getPut().getTags().get(0)+"/"+path.getPut().getOperationId();
        }
        if(null!= path.getPatch()){
            return path.getPatch().getTags().get(0)+"/"+path.getPatch().getOperationId();
        }
        return "";
    }

    private static Map<String,Model> definitions = new HashMap<>();


    /**
     * 将模型与接口文档组合
     * @param swagger
     * @param path
     * @return
     */
    private static String doPathWithParameter(Swagger swagger,Path path){

        StringBuilder builder = new StringBuilder();

        // 请求报文模型处理
        if(path.getPost() != null){
            // POST 请求报文处理
            List<Parameter> postParameters = path.getPost().getParameters();
            postAndPutModelDoc(swagger, builder, postParameters);
            getParAndChangeId(swagger, builder, path.getPost().getResponses());
        }

        if( null != path.getPut() ){
            List<Parameter> putParameters = path.getPut().getParameters();
            postAndPutModelDoc(swagger, builder, putParameters);
            getParAndChangeId(swagger, builder, path.getPut().getResponses());
        }

        // 响应报文处理
        if(null != path.getGet()){
            getParAndChangeId(swagger, builder, path.getGet().getResponses());
        }
        if(null!= path.getHead()){
            getParAndChangeId(swagger, builder, path.getHead().getResponses());
        }
        if(null!= path.getDelete()){
            getParAndChangeId(swagger, builder, path.getDelete().getResponses());
        }
        if(null!= path.getOptions()){
            getParAndChangeId(swagger, builder, path.getOptions().getResponses());
        }
        if(null!= path.getPatch()){
            getParAndChangeId(swagger, builder, path.getPatch().getResponses());
        }

        return builder.toString();
    }

    private static void postAndPutModelDoc(Swagger swagger, StringBuilder builder, List<Parameter> parameters) {
        AtomicReference<Model> backModel = new AtomicReference<>();

        if(parameters != null && !parameters.isEmpty()){
            parameters.forEach(parameter -> {
                if(parameter instanceof BodyParameter){
                    BodyParameter bop = (BodyParameter)parameter;

                    if(bop.getSchema() instanceof RefModel){
                        RefModel refModel = (RefModel)bop.getSchema();
                        String simpleRef = refModel.getSimpleRef();
                        log.info(simpleRef);

                        Model model1 = definitions.get(simpleRef);
                        String s = SwaggerUtils.definitionsDocumentGenerateMd(swagger, simpleRef, model1);
                        builder.append(StringUtils.replace(s, "name=", "id="));
                        modelDoc(swagger,builder,model1);
                    }

                    if(bop.getSchema() instanceof ModelImpl){
                        ModelImpl model = (ModelImpl)bop.getSchema();
                        modelDocAddJdd(swagger,builder,backModel,model.getProperties());
                    }


                    if(bop.getSchema() instanceof ArrayModel){
                        ArrayModel arrayModel = (ArrayModel)bop.getSchema();
                        modelDocAdd(swagger,builder,backModel,arrayModel.getItems());

                        if(null != backModel.get()){
                            modelDoc(swagger,builder,backModel.get());
                        }
                    }

                }
            });
        }
    }

    private static void getParAndChangeId(Swagger swagger, StringBuilder builder, Map<String, Response> responses) {
        if(null!= responses){
            responses.forEach((k,response)-> modelDoc(swagger, builder, response.getResponseSchema()));
        }
    }


    /**
     * 递归处理参数模型
     * @param swagger
     * @param builder
     * @param model
     */
    private static void modelDoc(Swagger swagger, StringBuilder builder , Model model){
        AtomicReference<Model> backModel = new AtomicReference<>();

        if (null != model){
            if(model instanceof RefModel){
                RefModel refModel = (RefModel)model;

                Map<String, Property> properties = refModel.getProperties();
                String simpleRef = refModel.getSimpleRef();

                if( null == properties && StringUtils.isNotBlank(simpleRef)){

                    backModel.set(SwaggerUtils.definitions.get(simpleRef));

                    String s = SwaggerUtils.definitionsDocumentGenerateMd(swagger, simpleRef, backModel.get());
                    builder.append(StringUtils.replace(s, "name=", "id="));

                    if(null != backModel.get()){
                        modelDoc(swagger,builder,backModel.get());
                    }
                }

            }

            if(model instanceof ComposedModel){
                ComposedModel composedModel = (ComposedModel)model;
                List<Model> allOfModel = composedModel.getAllOf();
                allOfModel.forEach(oneOfModel -> modelDoc(swagger,builder,oneOfModel));

            }

            if(model instanceof ArrayModel){
                ArrayModel arrayModel = (ArrayModel)model;
                modelDocAdd(swagger,builder,backModel,arrayModel.getItems());
            }

            Map<String, Property> properties = model.getProperties();

            modelDocAddJdd(swagger,builder,backModel,properties);
        }
    }

    private static void modelDocAdd(Swagger swagger, StringBuilder builder, AtomicReference<Model> backModel, Property property) {
        if(property instanceof RefProperty){
            RefProperty refProperty = (RefProperty)property;
            String simpleRef = refProperty.getSimpleRef();

            backModel.set(SwaggerUtils.definitions.get(simpleRef));
            String modelDoc = SwaggerUtils.definitionsDocumentGenerateMd(swagger, simpleRef, backModel.get());
            builder.append(StringUtils.replace(modelDoc, "name=", "id="));
        }
    }

    private static void modelDocAddJdd(Swagger swagger, StringBuilder builder, AtomicReference<Model> backModel, Map<String, Property> properties){
        if(null != properties){
            properties.forEach((zd,property)->{

                modelDocAdd(swagger, builder, backModel, property);
                if(null != backModel.get()){
                    modelDoc(swagger,builder,backModel.get());
                }

                if(property instanceof ObjectProperty){
                    ObjectProperty objectProperty = (ObjectProperty)property;
                    Map<String, Property> objectProperties = objectProperty.getProperties();
                    modelDocAddJdd(swagger,builder,backModel,objectProperties);
                }

                if(property instanceof ArrayProperty){
                    ArrayProperty arrayProperty = (ArrayProperty)property;
                    Property items = arrayProperty.getItems();
                    modelDocAdd(swagger, builder, backModel, items);

                    if(null != backModel.get()){
                        modelDoc(swagger,builder,backModel.get());
                    }
                }



            });
        }
    }


    private static String generateMd(Swagger swagger, String k, Path swaggerPath,String swaggerUiUrl){
        StringBuilder builder = new StringBuilder();

        Map<String, Path> paths = new HashMap<>();
        paths.put(k,swaggerPath);
        swagger.setPaths(paths);

        Swagger2MarkupConverter swagger2MarkupConverter = XforcceSwagger2MarkupConverter.from(swagger).withConfig(config).build();
        Swagger2MarkupConverter.Context context = swagger2MarkupConverter.getContext();
        XforcceSwagger2MarkupConverter xforcceSwagger2MarkupConverter = new XforcceSwagger2MarkupConverter(context);

        builder.append(xforcceSwagger2MarkupConverter.toString()).append("\n").append("#### 在线调试").append("\n").append("[模拟调用](").append(swaggerUiUrl).append(getExternalDocs(swaggerPath)).append(")");

        String s = SwaggerUtils.doPathWithParameter(swagger, swaggerPath);
        builder.append(s);
        return builder.toString();
    }

    private static String overviewDocumentGenerateMd(Swagger swagger){
            Swagger2MarkupConverter swagger2MarkupConverter = XforcceSwagger2MarkupConverter.from(swagger).withConfig(config).build();
            Swagger2MarkupConverter.Context context = swagger2MarkupConverter.getContext();
            XforcceSwagger2MarkupConverter xforcceSwagger2MarkupConverter = new XforcceSwagger2MarkupConverter(context);
            return xforcceSwagger2MarkupConverter.overviewDocumenttoString();
    }

    private static String securityDocumentGenerateMd(Swagger swagger){
        Swagger2MarkupConverter swagger2MarkupConverter = XforcceSwagger2MarkupConverter.from(swagger).withConfig(config).build();
        Swagger2MarkupConverter.Context context = swagger2MarkupConverter.getContext();
        XforcceSwagger2MarkupConverter xforcceSwagger2MarkupConverter = new XforcceSwagger2MarkupConverter(context);
        return xforcceSwagger2MarkupConverter.securityDocumenttoString();
    }

    private static String definitionsDocumentGenerateMd(Swagger swagger,String modelKey,Model model){
        Map<String, Model> definitions = new HashMap<>();
        definitions.put(modelKey,model);
        swagger.setDefinitions(definitions);


        Swagger2MarkupConverter swagger2MarkupConverter = XforcceSwagger2MarkupConverter.from(swagger).withConfig(config).build();
        Swagger2MarkupConverter.Context context = swagger2MarkupConverter.getContext();
        XforcceSwagger2MarkupConverter xforcceSwagger2MarkupConverter = new XforcceSwagger2MarkupConverter(context);
        return xforcceSwagger2MarkupConverter.definitionsDocumenttoString();
    }


    private static void findApiByTag(Path path, Tag tag, List<Path> showDocPath){
        addShowDocByTag(path.getPost(),path,tag,showDocPath);
        addShowDocByTag(path.getGet(),path,tag,showDocPath);
        addShowDocByTag(path.getDelete(),path,tag,showDocPath);
        addShowDocByTag(path.getPut(),path,tag,showDocPath);
        addShowDocByTag(path.getOptions(),path,tag,showDocPath);
        addShowDocByTag(path.getHead(),path,tag,showDocPath);
        addShowDocByTag(path.getPatch(),path,tag,showDocPath);
    }

    private static void addShowDocByTag(Operation operation, Path path, Tag tag, List<Path> showDocPath){
        if(null!= operation && null != operation.getTags()){
            List<String> tagList = operation.getTags();

            tagList.forEach(tags ->{
                if(StringUtils.equals(tag.getName(),tags)){
                    showDocPath.add(path);
                }
            });
        }
    }

    private static String getApiDescription(Path path){
        if(null!= path.getPost()){
            return path.getPost().getSummary();
        }
        if(null!= path.getGet()){
            return path.getGet().getSummary();
        }
        if(null!= path.getHead()){
            return path.getHead().getSummary();
        }
        if(null!= path.getDelete()){
            return path.getDelete().getSummary();
        }
        if(null!= path.getOptions()){
            return path.getOptions().getSummary();
        }
        if(null!= path.getPut()){
            return path.getPut().getSummary();
        }
        if(null!= path.getPatch()){
            return path.getPatch().getSummary();
        }
        return "";
    }


    private static void doShowDoc(String showDocApiUrl,Path path, Tag tag, Swagger swagger, String apiPath,String apiKey,String apiToken,String swaggerUiUrl){
        if(null!= path.getPost() && path.getPost().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getGet() && path.getGet().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getPut() && path.getPut().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getOptions() && path.getOptions().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getDelete() && path.getDelete().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getPatch() && path.getPatch().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
        if(null!= path.getHead() && path.getHead().getTags().contains(tag.getName())){
            sendToShowDoc(showDocApiUrl,apiKey,apiToken,"接口列表",tag.getDescription(),getApiDescription(path),SwaggerUtils.generateMd(swagger,apiPath,path,swaggerUiUrl),"");
        }
    }

    /**
     * 将数据发送到ShowDoc
     * url:https://www.showdoc.cc/web/#/page/102098
     * @param showDocApiUrl showDoc文档地址(必填)
     * @param apiKey 认证凭证(必填)
     * @param apiToken 认证凭证(必填)
     * @param catName 当页面文档处于目录下时，请传递目录名（可空）
     * @param catNameSub 当页面文档处于更细分的子目录下时，请传递子目录名。（可空）
     * @param pageTitle 页面标题(必填)page_title存在则用内容更新，不存在则创建
     * @param pageContent 页面内容，可传递markdown格式的文本或者html源码(必填)
     * @param sNumber 页面序号。数字越小，该页面越靠前（可空）
     */
    private static void sendToShowDoc(String showDocApiUrl, String apiKey , String apiToken,String catName, String catNameSub, String pageTitle, String pageContent, String sNumber){
        Map<String,String> parMap = new HashMap<>();
        parMap.put("api_key",apiKey);
        parMap.put("api_token",apiToken);
        parMap.put("cat_name",catName);
        parMap.put("cat_name_sub",catNameSub);
        parMap.put("page_title",pageTitle);
        parMap.put("page_content",pageContent);
        parMap.put("s_number",sNumber);
        okHttpUtil.post(showDocApiUrl,parMap);
    }

    public static void updateToShowDoc(List ignoreApis) throws IOException {
        //TODO 增加排除特定接口后导入showDoc
    }


    /**
     * 更新文档到ShowDoc
     * @param showDocUrl showDoc地址
     * @param apiKey
     * @param apiToken
     * @param swagger
     */
    public static void updateToShowDoc(String showDocUrl,String apiKey, String apiToken, Swagger swagger,String swaggerUiUrl) {
        if(StringUtils.isEmpty(showDocUrl)){
            throw new RuntimeException("showDoc地址不能为空");
        }
        /**
         * showDoc更新文档api地址
         * 如果你使用开源版的showdoc ,则请求url为
         * http://你的域名/server/index.php?s=/api/item/updateByApi
         */
        if(!showDocUrl.contains("www.showdoc.cc")){
            showDocUrl = showDocUrl + "/server/index.php?s=/api/item/updateByApi";
        }else{
            showDocUrl="https://www.showdoc.cc/server/api/item/updateByApi";
        }
        String showDocApiUrl = showDocUrl;
        SwaggerUtils.definitions = swagger.getDefinitions();
        Map<String,List<Path>> showDocMap = new HashMap<>();


        List<Tag> tags = swagger.getTags();
        Map<String, Path> paths = swagger.getPaths();

        tags.forEach( tag -> {
            List<Path> showDocPath = new ArrayList<>();
            paths.forEach((k,v) ->{
                findApiByTag(v, tag, showDocPath);
                doShowDoc(showDocApiUrl,v,tag,swagger,k,apiKey,apiToken,swaggerUiUrl);
            });
            showDocMap.put(tag.getDescription(),showDocPath);
        });

        /**
         * 数据模型
         */
        if(null != SwaggerUtils.definitions){
            SwaggerUtils.definitions.forEach((k,v) -> sendToShowDoc(showDocApiUrl,apiKey,apiToken,"数据模型","",k,SwaggerUtils.definitionsDocumentGenerateMd(swagger,k,v),""));
        }

        /**
         * 概览
         */
        sendToShowDoc(showDocApiUrl,apiKey,apiToken,"","","概览",SwaggerUtils.overviewDocumentGenerateMd(swagger),"");

        /**
         * 安全
         */
        sendToShowDoc(showDocApiUrl,apiKey,apiToken,"","","安全",SwaggerUtils.securityDocumentGenerateMd(swagger),"");

    }
}
