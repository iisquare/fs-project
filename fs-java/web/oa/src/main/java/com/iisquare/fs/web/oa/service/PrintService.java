package com.iisquare.fs.web.oa.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.dao.PrintDao;
import com.iisquare.fs.web.oa.dao.PrintDocumentDao;
import com.iisquare.fs.web.oa.entity.Print;
import com.iisquare.fs.web.oa.entity.PrintDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class PrintService extends ServiceBase {

    @Autowired
    PrintDao printDao;
    @Autowired
    PrintDocumentDao printDocumentDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    PrintRenderService printRenderService;
    @Autowired
    FormFrameService formFrameService;
    @Autowired
    FormDataService formDataService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Print> data = printDao.findAll((Specification<Print>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if(!DPUtil.empty(param.get("frameId"))) {
                predicates.add(cb.equal(root.get("frameId"), DPUtil.parseInt(param.get("frameId"))));
            }
            if(!DPUtil.empty(param.get("status"))) {
                predicates.add(cb.equal(root.get("status"), DPUtil.parseInt(param.get("status"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"), new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        for (Object row : rows) {
            if (row instanceof Print) {
                Print info = (Print) row;
                info.setHasTemplate(Boolean.TRUE.equals(info.getHasTemplate()));
            }
        }
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withFormFrameInfo"))) {
            formFrameService.fillInfo(rows, "frameId");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Print info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Print> info = printDao.findById(id);
        if(info.isEmpty()) return null;
        Print data = info.get();
        data.setHasTemplate(Boolean.TRUE.equals(data.getHasTemplate()));
        return data;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        int frameId = DPUtil.parseInt(param.get("frameId"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        if(frameId > 0 && null == formFrameService.info(frameId)) {
            return ApiUtil.result(1005, "关联表单不存在", frameId);
        }
        Print info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Print();
            // 兼容历史表结构：以下字段非空但无数据库默认值。
            info.setJson("");
            info.setHtml("");
            info.setState("");
        }
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
        if(param.containsKey("content") || null == info.getId()) info.setContent(content);
        if(param.containsKey("description") || null == info.getId()) info.setDescription(description);
        if(param.containsKey("frameId") || null == info.getId()) info.setFrameId(frameId > 0 ? frameId : null);
        if(param.containsKey("sort") || null == info.getId()) info.setSort(sort);
        if(param.containsKey("status") || null == info.getId()) info.setStatus(status);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = printDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    @Transactional
    public Map<String, Object> saveWithDocument(Map<?, ?> param, MultipartFile file, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String fileError = documentFileError(file);
        if(null != fileError) return ApiUtil.result(1002, fileError, null);
        if(id > 0) return saveDocument(id, file, request);
        Map<String, Object> saved = save(param, request);
        if(!ApiUtil.succeed(saved)) return saved;
        Object data = saved.get("data");
        if(!(data instanceof Print)) return ApiUtil.result(500, "模板记录创建失败", data);
        return saveDocument(((Print) data).getId(), file, request);
    }

    @Transactional
    public Map<String, Object> saveDocument(Integer id, MultipartFile file, HttpServletRequest request) {
        if(id == null || id < 1) return ApiUtil.result(1001, "打印模板标识异常", id);
        String fileError = documentFileError(file);
        if(null != fileError) return ApiUtil.result(1002, fileError, null);
        String fileName = DPUtil.trim(DPUtil.parseString(file.getOriginalFilename()));
        Print info = printDao.findById(id).orElse(null);
        if(null == info) return ApiUtil.result(404, null, id);
        if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
        byte[] bytes;
        try {
            bytes = file.getBytes();
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                 XWPFDocument document = new XWPFDocument(input)) {
                if(document.getBodyElements().isEmpty()) {
                    return ApiUtil.result(1005, "Word模板内容为空", fileName);
                }
            }
        } catch (IOException | RuntimeException e) {
            return ApiUtil.result(1005, "无法解析Word模板，请确认文件未损坏且为 .docx 格式", e.getMessage());
        }
        String placeholders;
        try {
            placeholders = DPUtil.stringify(printRenderService.placeholders(bytes));
        } catch (Exception e) {
            placeholders = "[]";
        }
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        PrintDocument document = printDocumentDao.findById(id).orElseGet(PrintDocument::new);
        document.setPrintId(id);
        document.setTemplateData(bytes);
        document.setUpdatedTime(time);
        document.setUpdatedUid(uid);
        printDocumentDao.save(document);
        info.setTemplateName(fileName);
        info.setTemplateSize(file.getSize());
        info.setContent(placeholders);
        info.setHasTemplate(true);
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        info = printDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    private String documentFileError(MultipartFile file) {
        if(file == null || file.isEmpty()) return "请选择Word模板文件";
        if(file.getSize() > 20L * 1024 * 1024) return "Word模板不能超过20MB";
        String fileName = DPUtil.trim(DPUtil.parseString(file.getOriginalFilename()));
        if(DPUtil.empty(fileName) || !fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            return "仅支持 .docx 格式的Word模板";
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(file.getBytes());
             XWPFDocument document = new XWPFDocument(input)) {
            if(document.getBodyElements().isEmpty()) return "Word模板内容为空";
        } catch (IOException | RuntimeException e) {
            return "无法解析Word模板，请确认文件未损坏且为 .docx 格式";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public byte[] document(Integer id) {
        if(id == null || id < 1) return null;
        PrintDocument document = printDocumentDao.findById(id).orElse(null);
        return null == document ? null : document.getTemplateData();
    }

    public List<Map<String, Object>> fields(Integer id, Integer frameId) {
        Print info = info(id);
        if((null == frameId || frameId < 1) && null != info && null != info.getFrameId()) {
            frameId = info.getFrameId();
        }
        if(null == frameId || frameId < 1) return Collections.emptyList();
        return printRenderService.fields(formFrameService.info(frameId));
    }

    public Map<String, Object> sample(Integer id) {
        return printRenderService.sample(fields(id, null));
    }

    public Map<String, Object> render(Integer id, String dataId, Map<?, ?> data, boolean sampleData) {
        Print info = info(id);
        if(null == info) return ApiUtil.result(404, null, id);
        boolean hasBusinessData = !DPUtil.empty(dataId) || (null != data && !data.isEmpty());
        if(Integer.valueOf(2).equals(info.getStatus()) && hasBusinessData) {
            return ApiUtil.result(1009, "打印模板已禁用", id);
        }
        byte[] source = document(id);
        if(null == source || source.length < 1) return ApiUtil.result(1006, "模板尚未上传Word文件", id);
        Map<String, Object> model;
        if(!DPUtil.empty(dataId)) {
            Document record = formDataService.info(dataId);
            if(null == record) return ApiUtil.result(1007, "表单数据不存在", dataId);
            if(null != info.getFrameId() && !info.getFrameId().equals(record.getInteger("frameId"))) {
                return ApiUtil.result(1008, "表单数据与打印模板不匹配", DPUtil.buildMap(
                        String.class, Object.class, "templateFrameId", info.getFrameId(),
                        "recordFrameId", record.getInteger("frameId")
                ));
            }
            model = DPUtil.toJSON(record, Map.class);
        } else if(null != data && !data.isEmpty()) {
            model = DPUtil.toJSON(data, Map.class);
        } else {
            model = sampleData ? sample(id) : new LinkedHashMap<>();
        }
        if(null == model) model = new LinkedHashMap<>();
        model = normalizeModel(model);
        printRenderService.decorate(model);
        List<Map<String, Object>> printFields = fields(id, null);
        model = printRenderService.aliasFieldData(model, printFields);
        List<String> listFields = printRenderService.listFields(printFields);
        try {
            List<String> unresolved = printRenderService.unresolved(source, model);
            byte[] output = printRenderService.render(source, model, listFields);
            String recordName = DPUtil.parseString(model.get(MongoCore.FIELD_ID));
            String baseName = DPUtil.trim(DPUtil.parseString(info.getName()));
            if(DPUtil.empty(baseName)) baseName = "打印结果";
            String fileName = baseName + (DPUtil.empty(recordName) ? "" : "-" + recordName) + ".docx";
            return ApiUtil.result(0, null, DPUtil.buildMap(
                    String.class, Object.class,
                    "fileName", fileName.replaceAll("[\\\\/:*?\"<>|]", "_"),
                    "mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "size", output.length,
                    "content", Base64.getEncoder().encodeToString(output),
                    "placeholders", printRenderService.placeholders(source),
                    "outline", printRenderService.outline(output),
                    "unresolved", unresolved
            ));
        } catch (Exception e) {
            return ApiUtil.result(1100, "Word模板渲染失败", rootMessage(e));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while(null != cause.getCause() && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return DPUtil.empty(cause.getMessage()) ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private Map<String, Object> normalizeModel(Map<String, Object> model) {
        // 兼容部分历史数据或调用方将业务字段包在 content/form 中的结构。
        for (String wrapper : Arrays.asList("content", "form")) {
            Object value = model.get(wrapper);
            if(!(value instanceof Map)) continue;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = DPUtil.parseString(entry.getKey());
                if(!DPUtil.empty(key)) model.putIfAbsent(key, entry.getValue());
            }
        }
        return model;
    }

    public Map<String, Object> delete(List<Integer> ids, int uid) {
        if (null == ids || ids.size() < 1) return ApiUtil.result(0, null, 0);
        try {
            List<Print> list = printDao.findAllById(ids);
            long time = System.currentTimeMillis();
            for (Print item : list) {
                item.setStatus(-1);
                item.setUpdatedTime(time);
                item.setUpdatedUid(uid);
            }
            printDao.saveAll(list);
            return ApiUtil.result(0, null, list.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除打印模板失败", e.getMessage());
        }
    }

}
