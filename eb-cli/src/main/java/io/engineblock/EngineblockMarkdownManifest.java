package io.engineblock;

import io.virtdata.annotations.Service;
import io.virtdata.docsys.api.Docs;
import io.virtdata.docsys.api.DocsInfo;
import io.virtdata.docsys.api.DocsysDynamicManifest;

@Service(DocsysDynamicManifest.class)
public class EngineblockMarkdownManifest implements DocsysDynamicManifest {
    @Override
    public DocsInfo getDocs() {
        return new Docs().namespace("docs-for-eb")
                .addFirstFoundPath("eb-cli/src/main/resources/docs-for-eb/",
                        "docs-for-eb/").asDocsInfo();
    }
}
