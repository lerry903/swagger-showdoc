package com.lerry.swaggershowdoc.swagger;


import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.internal.document.DefinitionsDocument;
import io.github.swagger2markup.internal.document.OverviewDocument;
import io.github.swagger2markup.internal.document.PathsDocument;
import io.github.swagger2markup.internal.document.SecurityDocument;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;

public class XforcceSwagger2MarkupConverter extends Swagger2MarkupConverter {


    private final Context context;
    private final OverviewDocument overviewDocument;
    private final PathsDocument pathsDocument;
    private final DefinitionsDocument definitionsDocument;
    private final SecurityDocument securityDocument;

    public XforcceSwagger2MarkupConverter(Context context) {
        super(context);
        this.context = context;
        this.overviewDocument = new OverviewDocument(context);
        this.pathsDocument = new PathsDocument(context);
        this.definitionsDocument = new DefinitionsDocument(context);
        this.securityDocument = new SecurityDocument(context);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(applyPathsDocument().toString());
        return sb.toString();
    }

    public String overviewDocumenttoString() {
        StringBuilder sb = new StringBuilder();
        sb.append(applyOverviewDocument().toString());
        return sb.toString();
    }

    public String definitionsDocumenttoString() {
        StringBuilder sb = new StringBuilder();
        sb.append(applyDefinitionsDocument().toString());
        return sb.toString();
    }

    public String securityDocumenttoString() {
        StringBuilder sb = new StringBuilder();
        sb.append(applySecurityDocument().toString());
        return sb.toString();
    }



    private MarkupDocBuilder applyOverviewDocument() {
        return overviewDocument.apply(
                context.createMarkupDocBuilder(),
                OverviewDocument.parameters(context.getSwagger()));
    }

    private MarkupDocBuilder applyPathsDocument() {
        return pathsDocument.apply(
                context.createMarkupDocBuilder(),
                PathsDocument.parameters(context.getSwagger().getPaths()));
    }

    private MarkupDocBuilder applyDefinitionsDocument() {
        return definitionsDocument.apply(
                context.createMarkupDocBuilder(),
                DefinitionsDocument.parameters(context.getSwagger().getDefinitions()));
    }

    private MarkupDocBuilder applySecurityDocument() {
        return securityDocument.apply(
                context.createMarkupDocBuilder(),
                SecurityDocument.parameters(context.getSwagger().getSecurityDefinitions()));
    }

}
