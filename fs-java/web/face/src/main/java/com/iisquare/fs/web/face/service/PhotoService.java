package com.iisquare.fs.web.face.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.XlabRpc;
import com.iisquare.fs.web.face.dao.PhotoDao;
import com.iisquare.fs.web.face.entity.Photo;
import com.iisquare.fs.web.face.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class PhotoService extends ServiceBase {

    @Autowired
    private PhotoDao photoDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private UserService userService;
    @Autowired
    private XlabRpc xlabRpc;

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
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

    public Photo info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Photo> info = photoDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        Integer userId = ValidateUtil.filterInteger(param.get("userId"), true, 1, null, 0);
        if (userId < 1) return ApiUtil.result(1002, "人员ID异常", userId);
        String base64 = DPUtil.trim(DPUtil.parseString(param.get("base64")));
        if(DPUtil.empty(base64)) return ApiUtil.result(1003, "请选择文件上传人像图片", base64);
        int sort = DPUtil.parseInt(param.get("sort"));
        int cover = DPUtil.parseBoolean(param.get("cover")) ? 1 : 0;
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        Photo info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Photo();
        }
        if (!base64.equals(info.getBase64())) {
            JsonNode data = RpcUtil.data(xlabRpc.post("/face/detect", DPUtil.buildMap(
                    "image", base64,
                    "maxFaceNumber", 1
            )), true);
            if (null == data) return ApiUtil.result(2001, "调用识别服务失败", data);
            if (data.size() < 1) return ApiUtil.result(2002, "未检测到人脸区域", data);
            if (data.size() > 1) return ApiUtil.result(2003, "检测到多个人脸区域", data);
            data = data.get(0);
            info.setBase64(base64);
            info.setFace(data.get("face").asText());
            info.setBox(DPUtil.stringify(data.get("box")));
            info.setSquare(DPUtil.stringify(data.get("square")));
            info.setLandmark(DPUtil.stringify(data.get("landmark")));
            info.setEigenvalue(DPUtil.stringify(data.get("eigenvalue")));
        }
        info.setUserId(userId);
        info.setName(name);
        info.setSort(sort);
        info.setCover(cover);
        info.setStatus(status);
        info.setDescription(description);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        photoDao.revertCoverByUserId(info.getUserId());
        info = photoDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, -1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Specification spec = new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                int id = DPUtil.parseInt(param.get("id"));
                if(id > 0) predicates.add(cb.equal(root.get("id"), id));
                int userId = DPUtil.parseInt(param.get("userId"));
                if(userId > 0) predicates.add(cb.equal(root.get("userId"), userId));
                int status = DPUtil.parseInt(param.get("status"));
                if(!"".equals(DPUtil.parseString(param.get("status")))) {
                    predicates.add(cb.equal(root.get("status"), status));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), -1));
                }
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
        List<?> rows = null;
        long total = 0;
        switch (pageSize) {
            case 0:
                total = photoDao.count(spec);
                break;
            case -1:
                total = photoDao.count(spec);
                rows = photoDao.findAll(spec);
                break;
            default:
                Page<?> data = photoDao.findAll(spec, PageRequest.of(page - 1, pageSize, sort));
                rows = data.getContent();
                total = data.getTotalElements();
        }
        if(!DPUtil.empty(args.get("withInfo"))) {
            userService.fillInfo(rows, "userId");
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        photoDao.deleteInBatch(photoDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Photo> list = photoDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Photo item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        photoDao.saveAll(list);
        return true;
    }

    public List<User> fillCover(List<User> list) {
        if(null == list || list.size() < 1) return list;
        Set<Integer> ids = new HashSet<>();
        for (User item : list) {
            ids.add(item.getId());
        }
        List<Photo> photos = photoDao.findCoverByUserId(ids);
        Map<Integer, Photo> map = DPUtil.list2map(photos, Integer.class, Photo.class, "userId");
        for (User item : list) {
            Photo photo = map.get(item.getId());
            if (null == photo) continue;
            item.setFace(photo.getFace());
        }
        return list;
    }

}
