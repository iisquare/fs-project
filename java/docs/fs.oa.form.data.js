/*
 Navicat Premium Data Transfer

 Source Server         : localhost_27017
 Source Server Type    : MongoDB
 Source Server Version : 40405
 Source Host           : localhost:27017
 Source Schema         : fs_project

 Target Server Type    : MongoDB
 Target Server Version : 40405
 File Encoding         : 65001

 Date: 06/05/2021 17:29:15
*/


// ----------------------------
// Collection structure for fs.oa.form.data
// ----------------------------
db.getCollection("fs.oa.form.data").drop();
db.createCollection("fs.oa.form.data");
db.getCollection("fs.oa.form.data").createIndex({
    frameId: NumberInt("1")
}, {
    name: "idx_frame_id"
});
db.getCollection("fs.oa.form.data").createIndex({
    bpmInstance: NumberInt("1")
}, {
    name: "idx_bpm_instance"
});
db.getCollection("fs.oa.form.data").createIndex({
    bpmStatus: NumberInt("1")
}, {
    name: "idx_bpm_status"
});
db.getCollection("fs.oa.form.data").createIndex({
    bpmTask: NumberInt("1")
}, {
    name: "idx_bpm_task"
});
db.getCollection("fs.oa.form.data").createIndex({
    createdTime: NumberInt("1")
}, {
    name: "idx_created_time"
});
db.getCollection("fs.oa.form.data").createIndex({
    createdUid: NumberInt("1")
}, {
    name: "idx_created_uid"
});
db.getCollection("fs.oa.form.data").createIndex({
    updatedTime: NumberInt("1")
}, {
    name: "idx_updated_time"
});
db.getCollection("fs.oa.form.data").createIndex({
    updatedUid: NumberInt("1")
}, {
    name: "idx_updated_uid"
});
