package com.iisquare.fs.web.agent.storage;

import org.junit.Assert;
import org.junit.Test;

public class AgentStorageTest {

    @Test
    public void usesFsAgentAsDefaultBucket() {
        Assert.assertEquals("fs-agent", new AgentStorage("").bucket());
        Assert.assertEquals("fs-agent", new AgentStorage("  ").bucket());
    }

    @Test
    public void keepsConfiguredBucket() {
        Assert.assertEquals("custom-agent", new AgentStorage(" custom-agent ").bucket());
    }

    @Test
    public void usesBusinessSceneDirectoriesAtBucketRoot() {
        AgentStorage storage = new AgentStorage("fs-agent");

        Assert.assertEquals("bot-uploads/20261113/file.png",
                storage.botUpload("20261113", "file", "png"));
        Assert.assertEquals("agentic-uploads/20261113/file.json",
                storage.agenticUpload("20261113", "file", ".json"));
        Assert.assertEquals("client-versions/9/setup.exe",
                storage.clientVersion(9, "setup.exe"));
        Assert.assertEquals("knowledge-documents/2/20261113/doc.pdf",
                storage.knowledgeDocument(2, "20261113", "doc.pdf"));
        Assert.assertEquals("knowledge-images/2/20261113/image.png",
                storage.knowledgeImage(2, "20261113", "image", "png"));
        Assert.assertEquals("skill-versions/3/7.zip",
                storage.skillVersion(3, 7));
        Assert.assertEquals("bot-uploads/20261113/file..bin",
                storage.botUpload("20261113", "file.", "bin"));
    }

    @Test
    public void rejectsEmptyDirectoryAndParentTraversal() {
        AgentStorage storage = new AgentStorage("fs-agent");

        Assert.assertThrows(IllegalArgumentException.class,
                () -> storage.path("", "file"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> storage.path("bot-uploads", "../file"));
    }
}
