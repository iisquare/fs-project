package com.iisquare.fs.web.worker.core;

import com.iisquare.fs.web.worker.service.TaskService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

public class WatchListener implements TreeCacheListener {

    private TaskService node;

    public WatchListener(TaskService nodeService) {
        this.node = nodeService;
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        ChildData data = event.getData();
        if (null == data) return;
        String path = data.getPath();
        if (path.startsWith("/runtime/task/")) {
            byte[] message = data.getData();
            if (null == message) return;
            Task task = Task.decode(new String(message));
            if (null == task) return;
            switch (event.getType()) {
                case NODE_UPDATED:
                    if (task.isRunning()) {
                        node.containerService.start(task.getQueueName());
                    } else {
                        node.containerService.stop(task.getQueueName());
                    }
                    break;
                case NODE_REMOVED:
                    node.containerService.remove(task.getQueueName());
                    break;
            }
        }
    }
}
