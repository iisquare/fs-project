package com.iisquare.fs.web.crawler.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.crawler.service.NodeService;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;

public class WatchListener implements CuratorCacheListener {

    final NodeService nodeService;

    public WatchListener(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public void event(Type type, ChildData oldData, ChildData data) {
        if (null == data) return;
        String path = data.getPath();
        if (path.equals("/runtime/notice")) {
            ZooNotice notice = ZooNotice.decode(new String(data.getData(), ZooKeeperClient.charset));
            if (null == notice) return;
            switch (notice.action) {
                case "startNode": {
                    String nodeId = notice.args.at("/nodeId").asText();
                    if (DPUtil.empty(nodeId) || nodeId.equals(nodeService.nodeId())) {
                        nodeService.start();
                    }
                    break;
                }
                case "stopNode": {
                    String nodeId = notice.args.at("/nodeId").asText();
                    if (DPUtil.empty(nodeId) || nodeId.equals(nodeService.nodeId())) {
                        nodeService.stop();
                    }
                    break;
                }
            }
        }
    }
}
