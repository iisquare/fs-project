package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.entity.Provider;
import org.junit.Assert;
import org.junit.Test;

public class ProviderServiceTest {

    @Test
    public void externalProviderInfoMasksSecretFields() {
        ObjectNode source = DPUtil.objectNode();
        source.put("id", 1);
        source.put("name", "供应商");
        source.put("token", "provider-secret");
        source.put("content", "{\"headers\":{\"Authorization\":\"secret\"}}");

        ObjectNode result = new TestProviderService().mask(source);

        Assert.assertTrue(result.at("/tokenConfigured").asBoolean());
        Assert.assertFalse(result.has("token"));
        Assert.assertFalse(result.has("content"));
        Assert.assertFalse(DPUtil.stringify(result).contains("provider-secret"));
    }

    @Test
    public void providerSaveResponseDoesNotContainRawToken() {
        Provider provider = Provider.builder()
                .id(1)
                .name("供应商")
                .token("provider-secret")
                .content("{\"headers\":{\"Authorization\":\"secret\"}}")
                .build();

        ObjectNode result = new TestProviderService().safe(provider);

        Assert.assertTrue(result.at("/tokenConfigured").asBoolean());
        Assert.assertFalse(result.has("token"));
        Assert.assertFalse(result.has("content"));
    }

    private static class TestProviderService extends ProviderService {
        ObjectNode mask(ObjectNode node) {
            return maskSecretFields(node);
        }

        ObjectNode safe(Provider provider) {
            return safeInfo(provider);
        }
    }
}
