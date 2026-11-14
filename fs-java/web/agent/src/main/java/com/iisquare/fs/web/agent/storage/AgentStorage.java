package com.iisquare.fs.web.agent.storage;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agent 文件存储约定：所有场景共用一个 S3 桶，场景目录直接位于桶根。
 *
 * <pre>
 * fs-agent/
 *   bot-uploads/...
 *   agentic-uploads/...
 *   knowledge-documents/...
 *   knowledge-images/...
 *   client-versions/...
 *   skill-versions/...
 * </pre>
 *
 * 文件服务仍以 archive 表中的 bucket + filepath 为读取依据；本类只负责新对象的桶和路径，
 * 不改写历史对象的定位信息，便于旧桶与新桶并行过渡。
 */
@Component
public class AgentStorage {

    public static final String DEFAULT_BUCKET = "fs-agent";

    private final String bucket;

    public AgentStorage(@Value("${fs.agent.storage.bucket:" + DEFAULT_BUCKET + "}") String bucket) {
        String value = DPUtil.trim(bucket);
        this.bucket = DPUtil.empty(value) ? DEFAULT_BUCKET : value;
    }

    /** 新文件的统一存储桶 */
    public String bucket() {
        return bucket;
    }

    /** 组装对象路径；具体场景由各业务方法给出唯一的顶层目录 */
    public String path(String directory, String... parts) {
        String root = safe(directory);
        if (root.isEmpty()) throw new IllegalArgumentException("存储目录不能为空");
        StringBuilder result = new StringBuilder(root);
        for (String part : parts) {
            String segment = safe(part);
            if (segment.isEmpty()) continue;
            result.append('/').append(segment);
        }
        return result.toString();
    }

    public String botUpload(String date, String id, String suffix) {
        return path("bot-uploads", date, id + suffix(suffix));
    }

    public String clientVersion(int versionId, String fileName) {
        return path("client-versions", String.valueOf(versionId), fileName);
    }

    public String agenticUpload(String date, String id, String suffix) {
        return path("agentic-uploads", date, id + suffix(suffix));
    }

    public String knowledgeDocument(int knowledgeId, String date, String name) {
        return path("knowledge-documents", String.valueOf(knowledgeId), date, name);
    }

    public String knowledgeImage(int knowledgeId, String date, String id, String suffix) {
        return path("knowledge-images", String.valueOf(knowledgeId), date, id + suffix(suffix));
    }

    public String skillVersion(int skillId, int versionId) {
        return path("skill-versions", String.valueOf(skillId), versionId + ".zip");
    }

    private String suffix(String value) {
        String suffix = DPUtil.trim(value);
        if (suffix.isEmpty()) return "";
        return startsWith(suffix, '.') ? suffix : "." + suffix;
    }

    private boolean startsWith(String value, char expected) {
        return !value.isEmpty() && value.charAt(0) == expected;
    }

    private String safe(String value) {
        String result = DPUtil.trim(DPUtil.parseString(value)).replace('\\', '/');
        while (startsWith(result, '/')) result = result.substring(1);
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        for (String segment : result.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("存储路径不能包含当前或上级目录：" + value);
            }
        }
        return result;
    }
}
