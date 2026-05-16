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

 Date: 03/08/2022 19:13:39
*/


// ----------------------------
// Collection structure for fs_oa_form_data
// ----------------------------
db.getCollection("fs_oa_form_data").drop();
db.createCollection("fs_oa_form_data");
db.getCollection("fs_oa_form_data").createIndex({
    frameId: NumberInt("1")
}, {
    name: "idx_frame_id"
});
db.getCollection("fs_oa_form_data").createIndex({
    createdTime: NumberInt("1")
}, {
    name: "idx_created_time"
});
db.getCollection("fs_oa_form_data").createIndex({
    createdUid: NumberInt("1")
}, {
    name: "idx_created_uid"
});
db.getCollection("fs_oa_form_data").createIndex({
    updatedTime: NumberInt("1")
}, {
    name: "idx_updated_time"
});
db.getCollection("fs_oa_form_data").createIndex({
    updatedUid: NumberInt("1")
}, {
    name: "idx_updated_uid"
});
db.getCollection("fs_oa_form_data").createIndex({
    bpmWorkflowId: NumberInt("1")
}, {
    name: "idx_bpm_workflow_id"
});
db.getCollection("fs_oa_form_data").createIndex({
    bpmInstanceId: NumberInt("1")
}, {
    name: "idx_bpm_instance_id"
});
db.getCollection("fs_oa_form_data").createIndex({
    bpmStartUserId: NumberInt("1")
}, {
    name: "idx_bpm_start_user_id"
});
