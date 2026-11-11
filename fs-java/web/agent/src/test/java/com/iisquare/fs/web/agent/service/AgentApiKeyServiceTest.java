package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
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

}
