package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.entity.AgentApiKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AgentApiKeyServiceTest {

    @Test
    public void generatedSecretUsesAgentPrefix() {
        AgentApiKeyService service = new AgentApiKeyService();
        String secret = service.generateSecret();

        Assert.assertTrue(secret.startsWith("agt_"));
        Assert.assertTrue(secret.contains("."));
        Assert.assertTrue(secret.length() > 32);
        Assert.assertTrue(secret.length() <= 64);
    }

    @Test
    public void generatedSecretsAreUnique() {
        AgentApiKeyService service = new AgentApiKeyService();

        Assert.assertNotEquals(
                service.generateSecret(),
                service.generateSecret());
    }

    @Test
    public void parsesAgenticIdsFromStoredTextAndFormattedArray() {
        AgentApiKeyService service = new AgentApiKeyService();

        List<Integer> plain = service.agenticIds(DPUtil.toJSON("2,5"));
        Assert.assertEquals(List.of(2, 5), plain);

        List<Integer> json = service.agenticIds(DPUtil.toJSON("[2,5]"));
        Assert.assertEquals(List.of(2, 5), json);

        ArrayNode array = DPUtil.arrayNode();
        array.add(2);
        array.add(5);
        Assert.assertEquals(List.of(2, 5), service.agenticIds(array));
    }

    @Test
    public void emptyAgenticIdsAllowsAllFlows() {
        AgentApiKeyService service = new AgentApiKeyService();
        AgentApiKey key = new AgentApiKey();
        key.setAgenticIds("");

        Assert.assertTrue(service.allows(key, 1));
        Assert.assertTrue(service.allows(key, 999));
    }

    @Test
    public void configuredAgenticIdsRestrictFlows() {
        AgentApiKeyService service = new AgentApiKeyService();
        AgentApiKey key = new AgentApiKey();
        key.setAgenticIds("2,5");

        Assert.assertTrue(service.allows(key, 2));
        Assert.assertTrue(service.allows(key, 5));
        Assert.assertFalse(service.allows(key, 3));
    }

}
