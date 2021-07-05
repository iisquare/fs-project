/*
 Navicat Premium Data Transfer

 Source Server         : yoyo_27017
 Source Server Type    : MongoDB
 Source Server Version : 40405
 Source Host           : 192.168.0.146:37017
 Source Schema         : fs_project

 Target Server Type    : MongoDB
 Target Server Version : 40405
 File Encoding         : 65001

 Date: 30/06/2021 14:55:14
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
db.getCollection("fs.oa.form.data").createIndex({
    bpmWorkflowId: NumberInt("1")
}, {
    name: "idx_bpm_workflow_id"
});
db.getCollection("fs.oa.form.data").createIndex({
    bpmInstanceId: NumberInt("1")
}, {
    name: "idx_bpm_instance_id"
});
db.getCollection("fs.oa.form.data").createIndex({
    bpmStartUserId: NumberInt("1")
}, {
    name: "idx_bpm_start_user_id"
});
