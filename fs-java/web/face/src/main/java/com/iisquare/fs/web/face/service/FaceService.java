package com.iisquare.fs.web.face.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.MathUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.face.dao.GroupDao;
import com.iisquare.fs.web.face.dao.PhotoDao;
import com.iisquare.fs.web.face.dao.RelationDao;
import com.iisquare.fs.web.face.dao.UserDao;
import com.iisquare.fs.web.face.entity.Photo;
import com.iisquare.fs.web.face.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FaceService extends ServiceBase {

    @Autowired
    private PhotoService photoService;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private PhotoDao photoDao;

    private long triggerLoading = 0;
    private long lastLoading = 0;
    private long lastCompare = 0;
    private long lastCompareElapsed = 0;
    private Map<Integer, List<List<Double>>> faces = new HashMap<>();

    private Map<Integer, List<List<Double>>> load() {
        Map<Integer, List<List<Double>>> result = new HashMap<>();
        List<Integer> groupIds = groupDao.findOnline();
        if (groupIds.isEmpty()) return result;
        List<Integer> aids = relationDao.findAidByBid("user_group", groupIds);
        if (aids.isEmpty()) return result;
        List<Integer> userIds = userDao.findOnline(aids);
        if (userIds.isEmpty()) return result;
        List<Photo> photos = photoDao.findOnline(userIds);
        if (photos.isEmpty()) return result;
        for (Photo photo : photos) {
            JsonNode eigenvalue = DPUtil.parseJSON(photo.getEigenvalue());
            if (null == eigenvalue || eigenvalue.isEmpty()) continue;
            List<List<Double>> list = result.get(photo.getUserId());
            if (null == list) {
                list = new ArrayList<>();
                result.put(photo.getUserId(), list);
            }
            List<Double> item = new ArrayList<>();
            Iterator<JsonNode> iterator = eigenvalue.iterator();
            while (iterator.hasNext()) {
                item.add(DPUtil.parseDouble(iterator.next()));
            }
            list.add(item);
        }
        return result;
    }

    public ObjectNode state() {
        ObjectNode state = DPUtil.objectNode();
        state.put("triggerLoading", triggerLoading);
        state.put("lastLoading", lastLoading);
        Map<Integer, List<List<Double>>> faces = this.faces;
        int faceSize = 0;
        for (Map.Entry<Integer, List<List<Double>>> entry : faces.entrySet()) {
            faceSize += entry.getValue().size();
        }
        state.put("userSize", faces.size());
        state.put("faceSize", faceSize);
        state.put("lastCompare", lastCompare);
        state.put("lastCompareElapsed", lastCompareElapsed);
        return state;
    }

    public boolean reload(boolean modeForce) {
        if (!modeForce && triggerLoading > 0) return false;
        synchronized (FaceService.class) {
            if (!modeForce && triggerLoading > 0) return false;
            triggerLoading = System.currentTimeMillis();
            faces = load();
            triggerLoading = 0;
            lastLoading = System.currentTimeMillis();
        }
        return true;
    }

    public double similarity(List<Double> va, List<Double> vb) {
        double similarity = MathUtil.similarity(va, vb);
        if (0.0 != similarity) similarity = (similarity + 1.0) / 2.0;
        return Math.abs(similarity);
    }

    public Map<Integer, Double> topN(List<Double> eigenvalue, double threshold, int topN) {
        Map<Integer, Double> result = new LinkedHashMap<>();
        Map<Integer, List<List<Double>>> faces = this.faces;
        for (Map.Entry<Integer, List<List<Double>>> entry : faces.entrySet()) {
            double similarity = 0.0;
            for (List<Double> item : entry.getValue()) {
                similarity = Math.max(similarity, similarity(eigenvalue, item));
            }
            if (similarity < threshold) continue;
            result.put(entry.getKey(), similarity);
        }
        if (result.size() <= 1) return result;
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(result.entrySet());
        entries.sort((o1, o2) -> (int) ((o2.getValue() - o1.getValue()) * 100000000));
        if (entries.size() > topN) entries = entries.subList(0, topN);
        result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public List<User> search(List<Double>  eigenvalue, int threshold, int topN) {
        lastCompare = System.currentTimeMillis();
        Map<Integer, Double> map = topN(eigenvalue, Double.valueOf(threshold) / 100, topN);
        lastCompareElapsed = System.currentTimeMillis() - lastCompare;
        if (map.isEmpty()) return new ArrayList<>();
        List<User> list = userDao.findAllByStatusAndIdIn(1, map.keySet());
        if (list.isEmpty()) return list;
        list = photoService.fillCover(list);
        List<User> result = new ArrayList<>(list.size());
        Map<Integer, User> userMap = DPUtil.list2map(list, Integer.class, User.class, "id");
        for (Map.Entry<Integer, Double> entry : map.entrySet()) {
            User user = userMap.get(entry.getKey());
            if (null == user) continue;
            user.setSimilarity(entry.getValue());
            result.add(user);
        }
        return result;
    }

}
