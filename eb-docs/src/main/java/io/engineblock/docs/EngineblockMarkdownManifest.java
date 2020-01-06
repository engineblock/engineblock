package io.engineblock.docs;

import io.virtdata.annotations.Service;
import io.virtdata.docsys.api.DocNameSpacesBinder;
import io.virtdata.docsys.api.Docs;
import io.virtdata.docsys.api.DocsysDynamicManifest;

@Service(DocsysDynamicManifest.class)
public class EngineblockMarkdownManifest implements DocsysDynamicManifest {
    @Override
    public DocNameSpacesBinder getDocs() {
        return new Docs().namespace("docs-for-eb")
                .addFirstFoundPath("eb-cli/src/main/resources/docs-for-eb/",
                        "docs-for-eb/")
                .setEnabledByDefault(false)
                .asDocsInfo();
    }
}
