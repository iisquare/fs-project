package com.iisquare.fs.web.xlab.service;

import com.iisquare.fs.base.core.util.OSUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.SortedMap;
import java.util.WeakHashMap;

@Service
public class MaskService extends ServiceBase {

    private WeakHashMap<String, Mat> maskMap;
    @Autowired
    public OpenCVService cvService;

    public MaskService() {
        this.maskMap = new WeakHashMap<>();
    }

    public void dump(VideoCapture capture) {
        System.out.println("CAP_PROP_POS_FRAMES:" + capture.get(Videoio.CAP_PROP_POS_FRAMES));
        System.out.println("CAP_PROP_POS_AVI_RATIO:" + capture.get(Videoio.CAP_PROP_POS_AVI_RATIO));
        System.out.println("CAP_PROP_FRAME_WIDTH:" + capture.get(Videoio.CAP_PROP_FRAME_WIDTH));
        System.out.println("CAP_PROP_FRAME_HEIGHT:" + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        System.out.println("CAP_PROP_FPS:" + capture.get(Videoio.CAP_PROP_FPS));
        System.out.println("CAP_PROP_FOURCC:" + capture.get(Videoio.CAP_PROP_FOURCC));
        System.out.println("CAP_PROP_FRAME_COUNT:" + capture.get(Videoio.CAP_PROP_FRAME_COUNT));
        System.out.println("CAP_PROP_FORMAT:" + capture.get(Videoio.CAP_PROP_FORMAT));
        System.out.println("CAP_PROP_MODE:" + capture.get(Videoio.CAP_PROP_MODE));
        System.out.println("CAP_PROP_BRIGHTNESS:" + capture.get(Videoio.CAP_PROP_BRIGHTNESS));
        System.out.println("CAP_PROP_CONTRAST:" + capture.get(Videoio.CAP_PROP_CONTRAST));
        System.out.println("CAP_PROP_SATURATION:" + capture.get(Videoio.CAP_PROP_SATURATION));
        System.out.println("CAP_PROP_HUE:" + capture.get(Videoio.CAP_PROP_HUE));
        System.out.println("CAP_PROP_GAIN:" + capture.get(Videoio.CAP_PROP_GAIN));
        System.out.println("CAP_PROP_EXPOSURE:" + capture.get(Videoio.CAP_PROP_EXPOSURE));
        System.out.println("CAP_PROP_CONVERT_RGB:" + capture.get(Videoio.CAP_PROP_CONVERT_RGB));
        System.out.println("CAP_PROP_WHITE_BALANCE_BLUE_U:" + capture.get(Videoio.CAP_PROP_WHITE_BALANCE_BLUE_U));
        System.out.println("CAP_PROP_RECTIFICATION:" + capture.get(Videoio.CAP_PROP_RECTIFICATION));
        System.out.println("CAP_PROP_MONOCHROME:" + capture.get(Videoio.CAP_PROP_MONOCHROME));
        System.out.println("CAP_PROP_SHARPNESS:" + capture.get(Videoio.CAP_PROP_SHARPNESS));
        System.out.println("CAP_PROP_AUTO_EXPOSURE:" + capture.get(Videoio.CAP_PROP_AUTO_EXPOSURE));
        System.out.println("CAP_PROP_GAMMA:" + capture.get(Videoio.CAP_PROP_GAMMA));
        System.out.println("CAP_PROP_TEMPERATURE:" + capture.get(Videoio.CAP_PROP_TEMPERATURE));
        System.out.println("CAP_PROP_TRIGGER:" + capture.get(Videoio.CAP_PROP_TRIGGER));
        System.out.println("CAP_PROP_TRIGGER_DELAY:" + capture.get(Videoio.CAP_PROP_TRIGGER_DELAY));
        System.out.println("CAP_PROP_WHITE_BALANCE_RED_V:" + capture.get(Videoio.CAP_PROP_WHITE_BALANCE_RED_V));
        System.out.println("CAP_PROP_ZOOM:" + capture.get(Videoio.CAP_PROP_ZOOM));
        System.out.println("CAP_PROP_FOCUS:" + capture.get(Videoio.CAP_PROP_FOCUS));
        System.out.println("CAP_PROP_GUID:" + capture.get(Videoio.CAP_PROP_GUID));
        System.out.println("CAP_PROP_ISO_SPEED:" + capture.get(Videoio.CAP_PROP_ISO_SPEED));
        System.out.println("CAP_PROP_BACKLIGHT:" + capture.get(Videoio.CAP_PROP_BACKLIGHT));
        System.out.println("CAP_PROP_PAN:" + capture.get(Videoio.CAP_PROP_PAN));
        System.out.println("CAP_PROP_TILT:" + capture.get(Videoio.CAP_PROP_TILT));
        System.out.println("CAP_PROP_ROLL:" + capture.get(Videoio.CAP_PROP_ROLL));
        System.out.println("CAP_PROP_IRIS:" + capture.get(Videoio.CAP_PROP_IRIS));
        System.out.println("CAP_PROP_SETTINGS:" + capture.get(Videoio.CAP_PROP_SETTINGS));
        System.out.println("CAP_PROP_BUFFERSIZE:" + capture.get(Videoio.CAP_PROP_BUFFERSIZE));
        System.out.println("CAP_PROP_AUTOFOCUS:" + capture.get(Videoio.CAP_PROP_AUTOFOCUS));
        System.out.println("CAP_PROP_SAR_NUM:" + capture.get(Videoio.CAP_PROP_SAR_NUM));
        System.out.println("CAP_PROP_SAR_DEN:" + capture.get(Videoio.CAP_PROP_SAR_DEN));
        System.out.println("CAP_PROP_BACKEND:" + capture.get(Videoio.CAP_PROP_BACKEND));
        System.out.println("CAP_PROP_CHANNEL:" + capture.get(Videoio.CAP_PROP_CHANNEL));
        System.out.println("CAP_PROP_AUTO_WB:" + capture.get(Videoio.CAP_PROP_AUTO_WB));
        System.out.println("CAP_PROP_WB_TEMPERATURE:" + capture.get(Videoio.CAP_PROP_WB_TEMPERATURE));
        System.out.println("CAP_PROP_DC1394_OFF:" + capture.get(Videoio.CAP_PROP_DC1394_OFF));
        System.out.println("CAP_PROP_DC1394_MODE_MANUAL:" + capture.get(Videoio.CAP_PROP_DC1394_MODE_MANUAL));
        System.out.println("CAP_PROP_DC1394_MODE_AUTO:" + capture.get(Videoio.CAP_PROP_DC1394_MODE_AUTO));
        System.out.println("CAP_PROP_DC1394_MODE_ONE_PUSH_AUTO:" + capture.get(Videoio.CAP_PROP_DC1394_MODE_ONE_PUSH_AUTO));
        System.out.println("CAP_PROP_DC1394_MAX:" + capture.get(Videoio.CAP_PROP_DC1394_MAX));
        System.out.println("CAP_OPENNI_DEPTH_GENERATOR:" + capture.get(Videoio.CAP_OPENNI_DEPTH_GENERATOR));
        System.out.println("CAP_OPENNI_IMAGE_GENERATOR:" + capture.get(Videoio.CAP_OPENNI_IMAGE_GENERATOR));
        System.out.println("CAP_OPENNI_IR_GENERATOR:" + capture.get(Videoio.CAP_OPENNI_IR_GENERATOR));
        System.out.println("CAP_OPENNI_GENERATORS_MASK:" + capture.get(Videoio.CAP_OPENNI_GENERATORS_MASK));
        System.out.println("CAP_PROP_OPENNI_OUTPUT_MODE:" + capture.get(Videoio.CAP_PROP_OPENNI_OUTPUT_MODE));
        System.out.println("CAP_PROP_OPENNI_FRAME_MAX_DEPTH:" + capture.get(Videoio.CAP_PROP_OPENNI_FRAME_MAX_DEPTH));
        System.out.println("CAP_PROP_OPENNI_BASELINE:" + capture.get(Videoio.CAP_PROP_OPENNI_BASELINE));
        System.out.println("CAP_PROP_OPENNI_FOCAL_LENGTH:" + capture.get(Videoio.CAP_PROP_OPENNI_FOCAL_LENGTH));
        System.out.println("CAP_PROP_OPENNI_REGISTRATION:" + capture.get(Videoio.CAP_PROP_OPENNI_REGISTRATION));
        System.out.println("CAP_PROP_OPENNI_REGISTRATION_ON:" + capture.get(Videoio.CAP_PROP_OPENNI_REGISTRATION_ON));
        System.out.println("CAP_PROP_OPENNI_APPROX_FRAME_SYNC:" + capture.get(Videoio.CAP_PROP_OPENNI_APPROX_FRAME_SYNC));
        System.out.println("CAP_PROP_OPENNI_MAX_BUFFER_SIZE:" + capture.get(Videoio.CAP_PROP_OPENNI_MAX_BUFFER_SIZE));
        System.out.println("CAP_PROP_OPENNI_CIRCLE_BUFFER:" + capture.get(Videoio.CAP_PROP_OPENNI_CIRCLE_BUFFER));
        System.out.println("CAP_PROP_OPENNI_MAX_TIME_DURATION:" + capture.get(Videoio.CAP_PROP_OPENNI_MAX_TIME_DURATION));
        System.out.println("CAP_PROP_OPENNI_GENERATOR_PRESENT:" + capture.get(Videoio.CAP_PROP_OPENNI_GENERATOR_PRESENT));
        System.out.println("CAP_PROP_OPENNI2_SYNC:" + capture.get(Videoio.CAP_PROP_OPENNI2_SYNC));
        System.out.println("CAP_PROP_OPENNI2_MIRROR:" + capture.get(Videoio.CAP_PROP_OPENNI2_MIRROR));
        System.out.println("CAP_PROP_GSTREAMER_QUEUE_LENGTH:" + capture.get(Videoio.CAP_PROP_GSTREAMER_QUEUE_LENGTH));
        System.out.println("CAP_PROP_PVAPI_MULTICASTIP:" + capture.get(Videoio.CAP_PROP_PVAPI_MULTICASTIP));
        System.out.println("CAP_PROP_PVAPI_FRAMESTARTTRIGGERMODE:" + capture.get(Videoio.CAP_PROP_PVAPI_FRAMESTARTTRIGGERMODE));
        System.out.println("CAP_PROP_PVAPI_DECIMATIONHORIZONTAL:" + capture.get(Videoio.CAP_PROP_PVAPI_DECIMATIONHORIZONTAL));
        System.out.println("CAP_PROP_PVAPI_DECIMATIONVERTICAL:" + capture.get(Videoio.CAP_PROP_PVAPI_DECIMATIONVERTICAL));
        System.out.println("CAP_PROP_PVAPI_BINNINGX:" + capture.get(Videoio.CAP_PROP_PVAPI_BINNINGX));
        System.out.println("CAP_PROP_PVAPI_BINNINGY:" + capture.get(Videoio.CAP_PROP_PVAPI_BINNINGY));
        System.out.println("CAP_PROP_PVAPI_PIXELFORMAT:" + capture.get(Videoio.CAP_PROP_PVAPI_PIXELFORMAT));
        System.out.println("CAP_PROP_XI_DOWNSAMPLING:" + capture.get(Videoio.CAP_PROP_XI_DOWNSAMPLING));
        System.out.println("CAP_PROP_XI_DATA_FORMAT:" + capture.get(Videoio.CAP_PROP_XI_DATA_FORMAT));
        System.out.println("CAP_PROP_XI_OFFSET_X:" + capture.get(Videoio.CAP_PROP_XI_OFFSET_X));
        System.out.println("CAP_PROP_XI_OFFSET_Y:" + capture.get(Videoio.CAP_PROP_XI_OFFSET_Y));
        System.out.println("CAP_PROP_XI_TRG_SOURCE:" + capture.get(Videoio.CAP_PROP_XI_TRG_SOURCE));
        System.out.println("CAP_PROP_XI_TRG_SOFTWARE:" + capture.get(Videoio.CAP_PROP_XI_TRG_SOFTWARE));
        System.out.println("CAP_PROP_XI_GPI_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_GPI_SELECTOR));
        System.out.println("CAP_PROP_XI_GPI_MODE:" + capture.get(Videoio.CAP_PROP_XI_GPI_MODE));
        System.out.println("CAP_PROP_XI_GPI_LEVEL:" + capture.get(Videoio.CAP_PROP_XI_GPI_LEVEL));
        System.out.println("CAP_PROP_XI_GPO_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_GPO_SELECTOR));
        System.out.println("CAP_PROP_XI_GPO_MODE:" + capture.get(Videoio.CAP_PROP_XI_GPO_MODE));
        System.out.println("CAP_PROP_XI_LED_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_LED_SELECTOR));
        System.out.println("CAP_PROP_XI_LED_MODE:" + capture.get(Videoio.CAP_PROP_XI_LED_MODE));
        System.out.println("CAP_PROP_XI_MANUAL_WB:" + capture.get(Videoio.CAP_PROP_XI_MANUAL_WB));
        System.out.println("CAP_PROP_XI_AUTO_WB:" + capture.get(Videoio.CAP_PROP_XI_AUTO_WB));
        System.out.println("CAP_PROP_XI_AEAG:" + capture.get(Videoio.CAP_PROP_XI_AEAG));
        System.out.println("CAP_PROP_XI_EXP_PRIORITY:" + capture.get(Videoio.CAP_PROP_XI_EXP_PRIORITY));
        System.out.println("CAP_PROP_XI_AE_MAX_LIMIT:" + capture.get(Videoio.CAP_PROP_XI_AE_MAX_LIMIT));
        System.out.println("CAP_PROP_XI_AG_MAX_LIMIT:" + capture.get(Videoio.CAP_PROP_XI_AG_MAX_LIMIT));
        System.out.println("CAP_PROP_XI_AEAG_LEVEL:" + capture.get(Videoio.CAP_PROP_XI_AEAG_LEVEL));
        System.out.println("CAP_PROP_XI_TIMEOUT:" + capture.get(Videoio.CAP_PROP_XI_TIMEOUT));
        System.out.println("CAP_PROP_XI_EXPOSURE:" + capture.get(Videoio.CAP_PROP_XI_EXPOSURE));
        System.out.println("CAP_PROP_XI_EXPOSURE_BURST_COUNT:" + capture.get(Videoio.CAP_PROP_XI_EXPOSURE_BURST_COUNT));
        System.out.println("CAP_PROP_XI_GAIN_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_GAIN_SELECTOR));
        System.out.println("CAP_PROP_XI_GAIN:" + capture.get(Videoio.CAP_PROP_XI_GAIN));
        System.out.println("CAP_PROP_XI_DOWNSAMPLING_TYPE:" + capture.get(Videoio.CAP_PROP_XI_DOWNSAMPLING_TYPE));
        System.out.println("CAP_PROP_XI_BINNING_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_BINNING_SELECTOR));
        System.out.println("CAP_PROP_XI_BINNING_VERTICAL:" + capture.get(Videoio.CAP_PROP_XI_BINNING_VERTICAL));
        System.out.println("CAP_PROP_XI_BINNING_HORIZONTAL:" + capture.get(Videoio.CAP_PROP_XI_BINNING_HORIZONTAL));
        System.out.println("CAP_PROP_XI_BINNING_PATTERN:" + capture.get(Videoio.CAP_PROP_XI_BINNING_PATTERN));
        System.out.println("CAP_PROP_XI_DECIMATION_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_DECIMATION_SELECTOR));
        System.out.println("CAP_PROP_XI_DECIMATION_VERTICAL:" + capture.get(Videoio.CAP_PROP_XI_DECIMATION_VERTICAL));
        System.out.println("CAP_PROP_XI_DECIMATION_HORIZONTAL:" + capture.get(Videoio.CAP_PROP_XI_DECIMATION_HORIZONTAL));
        System.out.println("CAP_PROP_XI_DECIMATION_PATTERN:" + capture.get(Videoio.CAP_PROP_XI_DECIMATION_PATTERN));
        System.out.println("CAP_PROP_XI_TEST_PATTERN_GENERATOR_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_TEST_PATTERN_GENERATOR_SELECTOR));
        System.out.println("CAP_PROP_XI_TEST_PATTERN:" + capture.get(Videoio.CAP_PROP_XI_TEST_PATTERN));
        System.out.println("CAP_PROP_XI_IMAGE_DATA_FORMAT:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_DATA_FORMAT));
        System.out.println("CAP_PROP_XI_SHUTTER_TYPE:" + capture.get(Videoio.CAP_PROP_XI_SHUTTER_TYPE));
        System.out.println("CAP_PROP_XI_SENSOR_TAPS:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_TAPS));
        System.out.println("CAP_PROP_XI_AEAG_ROI_OFFSET_X:" + capture.get(Videoio.CAP_PROP_XI_AEAG_ROI_OFFSET_X));
        System.out.println("CAP_PROP_XI_AEAG_ROI_OFFSET_Y:" + capture.get(Videoio.CAP_PROP_XI_AEAG_ROI_OFFSET_Y));
        System.out.println("CAP_PROP_XI_AEAG_ROI_WIDTH:" + capture.get(Videoio.CAP_PROP_XI_AEAG_ROI_WIDTH));
        System.out.println("CAP_PROP_XI_AEAG_ROI_HEIGHT:" + capture.get(Videoio.CAP_PROP_XI_AEAG_ROI_HEIGHT));
        System.out.println("CAP_PROP_XI_BPC:" + capture.get(Videoio.CAP_PROP_XI_BPC));
        System.out.println("CAP_PROP_XI_WB_KR:" + capture.get(Videoio.CAP_PROP_XI_WB_KR));
        System.out.println("CAP_PROP_XI_WB_KG:" + capture.get(Videoio.CAP_PROP_XI_WB_KG));
        System.out.println("CAP_PROP_XI_WB_KB:" + capture.get(Videoio.CAP_PROP_XI_WB_KB));
        System.out.println("CAP_PROP_XI_WIDTH:" + capture.get(Videoio.CAP_PROP_XI_WIDTH));
        System.out.println("CAP_PROP_XI_HEIGHT:" + capture.get(Videoio.CAP_PROP_XI_HEIGHT));
        System.out.println("CAP_PROP_XI_REGION_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_REGION_SELECTOR));
        System.out.println("CAP_PROP_XI_REGION_MODE:" + capture.get(Videoio.CAP_PROP_XI_REGION_MODE));
        System.out.println("CAP_PROP_XI_LIMIT_BANDWIDTH:" + capture.get(Videoio.CAP_PROP_XI_LIMIT_BANDWIDTH));
        System.out.println("CAP_PROP_XI_SENSOR_DATA_BIT_DEPTH:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_DATA_BIT_DEPTH));
        System.out.println("CAP_PROP_XI_OUTPUT_DATA_BIT_DEPTH:" + capture.get(Videoio.CAP_PROP_XI_OUTPUT_DATA_BIT_DEPTH));
        System.out.println("CAP_PROP_XI_IMAGE_DATA_BIT_DEPTH:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_DATA_BIT_DEPTH));
        System.out.println("CAP_PROP_XI_OUTPUT_DATA_PACKING:" + capture.get(Videoio.CAP_PROP_XI_OUTPUT_DATA_PACKING));
        System.out.println("CAP_PROP_XI_OUTPUT_DATA_PACKING_TYPE:" + capture.get(Videoio.CAP_PROP_XI_OUTPUT_DATA_PACKING_TYPE));
        System.out.println("CAP_PROP_XI_IS_COOLED:" + capture.get(Videoio.CAP_PROP_XI_IS_COOLED));
        System.out.println("CAP_PROP_XI_COOLING:" + capture.get(Videoio.CAP_PROP_XI_COOLING));
        System.out.println("CAP_PROP_XI_TARGET_TEMP:" + capture.get(Videoio.CAP_PROP_XI_TARGET_TEMP));
        System.out.println("CAP_PROP_XI_CHIP_TEMP:" + capture.get(Videoio.CAP_PROP_XI_CHIP_TEMP));
        System.out.println("CAP_PROP_XI_HOUS_TEMP:" + capture.get(Videoio.CAP_PROP_XI_HOUS_TEMP));
        System.out.println("CAP_PROP_XI_HOUS_BACK_SIDE_TEMP:" + capture.get(Videoio.CAP_PROP_XI_HOUS_BACK_SIDE_TEMP));
        System.out.println("CAP_PROP_XI_SENSOR_BOARD_TEMP:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_BOARD_TEMP));
        System.out.println("CAP_PROP_XI_CMS:" + capture.get(Videoio.CAP_PROP_XI_CMS));
        System.out.println("CAP_PROP_XI_APPLY_CMS:" + capture.get(Videoio.CAP_PROP_XI_APPLY_CMS));
        System.out.println("CAP_PROP_XI_IMAGE_IS_COLOR:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_IS_COLOR));
        System.out.println("CAP_PROP_XI_COLOR_FILTER_ARRAY:" + capture.get(Videoio.CAP_PROP_XI_COLOR_FILTER_ARRAY));
        System.out.println("CAP_PROP_XI_GAMMAY:" + capture.get(Videoio.CAP_PROP_XI_GAMMAY));
        System.out.println("CAP_PROP_XI_GAMMAC:" + capture.get(Videoio.CAP_PROP_XI_GAMMAC));
        System.out.println("CAP_PROP_XI_SHARPNESS:" + capture.get(Videoio.CAP_PROP_XI_SHARPNESS));
        System.out.println("CAP_PROP_XI_CC_MATRIX_00:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_00));
        System.out.println("CAP_PROP_XI_CC_MATRIX_01:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_01));
        System.out.println("CAP_PROP_XI_CC_MATRIX_02:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_02));
        System.out.println("CAP_PROP_XI_CC_MATRIX_03:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_03));
        System.out.println("CAP_PROP_XI_CC_MATRIX_10:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_10));
        System.out.println("CAP_PROP_XI_CC_MATRIX_11:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_11));
        System.out.println("CAP_PROP_XI_CC_MATRIX_12:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_12));
        System.out.println("CAP_PROP_XI_CC_MATRIX_13:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_13));
        System.out.println("CAP_PROP_XI_CC_MATRIX_20:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_20));
        System.out.println("CAP_PROP_XI_CC_MATRIX_21:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_21));
        System.out.println("CAP_PROP_XI_CC_MATRIX_22:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_22));
        System.out.println("CAP_PROP_XI_CC_MATRIX_23:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_23));
        System.out.println("CAP_PROP_XI_CC_MATRIX_30:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_30));
        System.out.println("CAP_PROP_XI_CC_MATRIX_31:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_31));
        System.out.println("CAP_PROP_XI_CC_MATRIX_32:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_32));
        System.out.println("CAP_PROP_XI_CC_MATRIX_33:" + capture.get(Videoio.CAP_PROP_XI_CC_MATRIX_33));
        System.out.println("CAP_PROP_XI_DEFAULT_CC_MATRIX:" + capture.get(Videoio.CAP_PROP_XI_DEFAULT_CC_MATRIX));
        System.out.println("CAP_PROP_XI_TRG_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_TRG_SELECTOR));
        System.out.println("CAP_PROP_XI_ACQ_FRAME_BURST_COUNT:" + capture.get(Videoio.CAP_PROP_XI_ACQ_FRAME_BURST_COUNT));
        System.out.println("CAP_PROP_XI_DEBOUNCE_EN:" + capture.get(Videoio.CAP_PROP_XI_DEBOUNCE_EN));
        System.out.println("CAP_PROP_XI_DEBOUNCE_T0:" + capture.get(Videoio.CAP_PROP_XI_DEBOUNCE_T0));
        System.out.println("CAP_PROP_XI_DEBOUNCE_T1:" + capture.get(Videoio.CAP_PROP_XI_DEBOUNCE_T1));
        System.out.println("CAP_PROP_XI_DEBOUNCE_POL:" + capture.get(Videoio.CAP_PROP_XI_DEBOUNCE_POL));
        System.out.println("CAP_PROP_XI_LENS_MODE:" + capture.get(Videoio.CAP_PROP_XI_LENS_MODE));
        System.out.println("CAP_PROP_XI_LENS_APERTURE_VALUE:" + capture.get(Videoio.CAP_PROP_XI_LENS_APERTURE_VALUE));
        System.out.println("CAP_PROP_XI_LENS_FOCUS_MOVEMENT_VALUE:" + capture.get(Videoio.CAP_PROP_XI_LENS_FOCUS_MOVEMENT_VALUE));
        System.out.println("CAP_PROP_XI_LENS_FOCUS_MOVE:" + capture.get(Videoio.CAP_PROP_XI_LENS_FOCUS_MOVE));
        System.out.println("CAP_PROP_XI_LENS_FOCUS_DISTANCE:" + capture.get(Videoio.CAP_PROP_XI_LENS_FOCUS_DISTANCE));
        System.out.println("CAP_PROP_XI_LENS_FOCAL_LENGTH:" + capture.get(Videoio.CAP_PROP_XI_LENS_FOCAL_LENGTH));
        System.out.println("CAP_PROP_XI_LENS_FEATURE_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_LENS_FEATURE_SELECTOR));
        System.out.println("CAP_PROP_XI_LENS_FEATURE:" + capture.get(Videoio.CAP_PROP_XI_LENS_FEATURE));
        System.out.println("CAP_PROP_XI_DEVICE_MODEL_ID:" + capture.get(Videoio.CAP_PROP_XI_DEVICE_MODEL_ID));
        System.out.println("CAP_PROP_XI_DEVICE_SN:" + capture.get(Videoio.CAP_PROP_XI_DEVICE_SN));
        System.out.println("CAP_PROP_XI_IMAGE_DATA_FORMAT_RGB32_ALPHA:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_DATA_FORMAT_RGB32_ALPHA));
        System.out.println("CAP_PROP_XI_IMAGE_PAYLOAD_SIZE:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_PAYLOAD_SIZE));
        System.out.println("CAP_PROP_XI_TRANSPORT_PIXEL_FORMAT:" + capture.get(Videoio.CAP_PROP_XI_TRANSPORT_PIXEL_FORMAT));
        System.out.println("CAP_PROP_XI_SENSOR_CLOCK_FREQ_HZ:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_CLOCK_FREQ_HZ));
        System.out.println("CAP_PROP_XI_SENSOR_CLOCK_FREQ_INDEX:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_CLOCK_FREQ_INDEX));
        System.out.println("CAP_PROP_XI_SENSOR_OUTPUT_CHANNEL_COUNT:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_OUTPUT_CHANNEL_COUNT));
        System.out.println("CAP_PROP_XI_FRAMERATE:" + capture.get(Videoio.CAP_PROP_XI_FRAMERATE));
        System.out.println("CAP_PROP_XI_COUNTER_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_COUNTER_SELECTOR));
        System.out.println("CAP_PROP_XI_COUNTER_VALUE:" + capture.get(Videoio.CAP_PROP_XI_COUNTER_VALUE));
        System.out.println("CAP_PROP_XI_ACQ_TIMING_MODE:" + capture.get(Videoio.CAP_PROP_XI_ACQ_TIMING_MODE));
        System.out.println("CAP_PROP_XI_AVAILABLE_BANDWIDTH:" + capture.get(Videoio.CAP_PROP_XI_AVAILABLE_BANDWIDTH));
        System.out.println("CAP_PROP_XI_BUFFER_POLICY:" + capture.get(Videoio.CAP_PROP_XI_BUFFER_POLICY));
        System.out.println("CAP_PROP_XI_LUT_EN:" + capture.get(Videoio.CAP_PROP_XI_LUT_EN));
        System.out.println("CAP_PROP_XI_LUT_INDEX:" + capture.get(Videoio.CAP_PROP_XI_LUT_INDEX));
        System.out.println("CAP_PROP_XI_LUT_VALUE:" + capture.get(Videoio.CAP_PROP_XI_LUT_VALUE));
        System.out.println("CAP_PROP_XI_TRG_DELAY:" + capture.get(Videoio.CAP_PROP_XI_TRG_DELAY));
        System.out.println("CAP_PROP_XI_TS_RST_MODE:" + capture.get(Videoio.CAP_PROP_XI_TS_RST_MODE));
        System.out.println("CAP_PROP_XI_TS_RST_SOURCE:" + capture.get(Videoio.CAP_PROP_XI_TS_RST_SOURCE));
        System.out.println("CAP_PROP_XI_IS_DEVICE_EXIST:" + capture.get(Videoio.CAP_PROP_XI_IS_DEVICE_EXIST));
        System.out.println("CAP_PROP_XI_ACQ_BUFFER_SIZE:" + capture.get(Videoio.CAP_PROP_XI_ACQ_BUFFER_SIZE));
        System.out.println("CAP_PROP_XI_ACQ_BUFFER_SIZE_UNIT:" + capture.get(Videoio.CAP_PROP_XI_ACQ_BUFFER_SIZE_UNIT));
        System.out.println("CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_SIZE:" + capture.get(Videoio.CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_SIZE));
        System.out.println("CAP_PROP_XI_BUFFERS_QUEUE_SIZE:" + capture.get(Videoio.CAP_PROP_XI_BUFFERS_QUEUE_SIZE));
        System.out.println("CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_COMMIT:" + capture.get(Videoio.CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_COMMIT));
        System.out.println("CAP_PROP_XI_RECENT_FRAME:" + capture.get(Videoio.CAP_PROP_XI_RECENT_FRAME));
        System.out.println("CAP_PROP_XI_DEVICE_RESET:" + capture.get(Videoio.CAP_PROP_XI_DEVICE_RESET));
        System.out.println("CAP_PROP_XI_COLUMN_FPN_CORRECTION:" + capture.get(Videoio.CAP_PROP_XI_COLUMN_FPN_CORRECTION));
        System.out.println("CAP_PROP_XI_ROW_FPN_CORRECTION:" + capture.get(Videoio.CAP_PROP_XI_ROW_FPN_CORRECTION));
        System.out.println("CAP_PROP_XI_SENSOR_MODE:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_MODE));
        System.out.println("CAP_PROP_XI_HDR:" + capture.get(Videoio.CAP_PROP_XI_HDR));
        System.out.println("CAP_PROP_XI_HDR_KNEEPOINT_COUNT:" + capture.get(Videoio.CAP_PROP_XI_HDR_KNEEPOINT_COUNT));
        System.out.println("CAP_PROP_XI_HDR_T1:" + capture.get(Videoio.CAP_PROP_XI_HDR_T1));
        System.out.println("CAP_PROP_XI_HDR_T2:" + capture.get(Videoio.CAP_PROP_XI_HDR_T2));
        System.out.println("CAP_PROP_XI_KNEEPOINT1:" + capture.get(Videoio.CAP_PROP_XI_KNEEPOINT1));
        System.out.println("CAP_PROP_XI_KNEEPOINT2:" + capture.get(Videoio.CAP_PROP_XI_KNEEPOINT2));
        System.out.println("CAP_PROP_XI_IMAGE_BLACK_LEVEL:" + capture.get(Videoio.CAP_PROP_XI_IMAGE_BLACK_LEVEL));
        System.out.println("CAP_PROP_XI_HW_REVISION:" + capture.get(Videoio.CAP_PROP_XI_HW_REVISION));
        System.out.println("CAP_PROP_XI_DEBUG_LEVEL:" + capture.get(Videoio.CAP_PROP_XI_DEBUG_LEVEL));
        System.out.println("CAP_PROP_XI_AUTO_BANDWIDTH_CALCULATION:" + capture.get(Videoio.CAP_PROP_XI_AUTO_BANDWIDTH_CALCULATION));
        System.out.println("CAP_PROP_XI_FFS_FILE_ID:" + capture.get(Videoio.CAP_PROP_XI_FFS_FILE_ID));
        System.out.println("CAP_PROP_XI_FFS_FILE_SIZE:" + capture.get(Videoio.CAP_PROP_XI_FFS_FILE_SIZE));
        System.out.println("CAP_PROP_XI_FREE_FFS_SIZE:" + capture.get(Videoio.CAP_PROP_XI_FREE_FFS_SIZE));
        System.out.println("CAP_PROP_XI_USED_FFS_SIZE:" + capture.get(Videoio.CAP_PROP_XI_USED_FFS_SIZE));
        System.out.println("CAP_PROP_XI_FFS_ACCESS_KEY:" + capture.get(Videoio.CAP_PROP_XI_FFS_ACCESS_KEY));
        System.out.println("CAP_PROP_XI_SENSOR_FEATURE_SELECTOR:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_FEATURE_SELECTOR));
        System.out.println("CAP_PROP_XI_SENSOR_FEATURE_VALUE:" + capture.get(Videoio.CAP_PROP_XI_SENSOR_FEATURE_VALUE));
        System.out.println("CAP_PROP_IOS_DEVICE_FOCUS:" + capture.get(Videoio.CAP_PROP_IOS_DEVICE_FOCUS));
        System.out.println("CAP_PROP_IOS_DEVICE_EXPOSURE:" + capture.get(Videoio.CAP_PROP_IOS_DEVICE_EXPOSURE));
        System.out.println("CAP_PROP_IOS_DEVICE_FLASH:" + capture.get(Videoio.CAP_PROP_IOS_DEVICE_FLASH));
        System.out.println("CAP_PROP_IOS_DEVICE_WHITEBALANCE:" + capture.get(Videoio.CAP_PROP_IOS_DEVICE_WHITEBALANCE));
        System.out.println("CAP_PROP_IOS_DEVICE_TORCH:" + capture.get(Videoio.CAP_PROP_IOS_DEVICE_TORCH));
        System.out.println("CAP_PROP_GIGA_FRAME_OFFSET_X:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_OFFSET_X));
        System.out.println("CAP_PROP_GIGA_FRAME_OFFSET_Y:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_OFFSET_Y));
        System.out.println("CAP_PROP_GIGA_FRAME_WIDTH_MAX:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_WIDTH_MAX));
        System.out.println("CAP_PROP_GIGA_FRAME_HEIGH_MAX:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_HEIGH_MAX));
        System.out.println("CAP_PROP_GIGA_FRAME_SENS_WIDTH:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_SENS_WIDTH));
        System.out.println("CAP_PROP_GIGA_FRAME_SENS_HEIGH:" + capture.get(Videoio.CAP_PROP_GIGA_FRAME_SENS_HEIGH));
        System.out.println("CAP_PROP_INTELPERC_PROFILE_COUNT:" + capture.get(Videoio.CAP_PROP_INTELPERC_PROFILE_COUNT));
        System.out.println("CAP_PROP_INTELPERC_PROFILE_IDX:" + capture.get(Videoio.CAP_PROP_INTELPERC_PROFILE_IDX));
        System.out.println("CAP_PROP_INTELPERC_DEPTH_LOW_CONFIDENCE_VALUE:" + capture.get(Videoio.CAP_PROP_INTELPERC_DEPTH_LOW_CONFIDENCE_VALUE));
        System.out.println("CAP_PROP_INTELPERC_DEPTH_SATURATION_VALUE:" + capture.get(Videoio.CAP_PROP_INTELPERC_DEPTH_SATURATION_VALUE));
        System.out.println("CAP_PROP_INTELPERC_DEPTH_CONFIDENCE_THRESHOLD:" + capture.get(Videoio.CAP_PROP_INTELPERC_DEPTH_CONFIDENCE_THRESHOLD));
        System.out.println("CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_HORZ:" + capture.get(Videoio.CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_HORZ));
        System.out.println("CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_VERT:" + capture.get(Videoio.CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_VERT));
        System.out.println("CAP_PROP_GPHOTO2_PREVIEW:" + capture.get(Videoio.CAP_PROP_GPHOTO2_PREVIEW));
        System.out.println("CAP_PROP_GPHOTO2_WIDGET_ENUMERATE:" + capture.get(Videoio.CAP_PROP_GPHOTO2_WIDGET_ENUMERATE));
        System.out.println("CAP_PROP_GPHOTO2_RELOAD_CONFIG:" + capture.get(Videoio.CAP_PROP_GPHOTO2_RELOAD_CONFIG));
        System.out.println("CAP_PROP_GPHOTO2_RELOAD_ON_CHANGE:" + capture.get(Videoio.CAP_PROP_GPHOTO2_RELOAD_ON_CHANGE));
        System.out.println("CAP_PROP_GPHOTO2_COLLECT_MSGS:" + capture.get(Videoio.CAP_PROP_GPHOTO2_COLLECT_MSGS));
        System.out.println("CAP_PROP_GPHOTO2_FLUSH_MSGS:" + capture.get(Videoio.CAP_PROP_GPHOTO2_FLUSH_MSGS));
        System.out.println("CAP_PROP_SPEED:" + capture.get(Videoio.CAP_PROP_SPEED));
        System.out.println("CAP_PROP_APERTURE:" + capture.get(Videoio.CAP_PROP_APERTURE));
        System.out.println("CAP_PROP_EXPOSUREPROGRAM:" + capture.get(Videoio.CAP_PROP_EXPOSUREPROGRAM));
        System.out.println("CAP_PROP_VIEWFINDER:" + capture.get(Videoio.CAP_PROP_VIEWFINDER));
        System.out.println("CAP_PROP_IMAGES_BASE:" + capture.get(Videoio.CAP_PROP_IMAGES_BASE));
        System.out.println("CAP_PROP_IMAGES_LAST:" + capture.get(Videoio.CAP_PROP_IMAGES_LAST));
    }

    public Mat position(Mat image) {
        Mat result = new Mat(image.size(), CvType.CV_8UC4);
        int mw = image.width(), mh = image.height();
        for (int row = 0; row < mh; row++) {
            for (int col = 0; col < mw; col++) {
                double[] ps = image.get(row, col);
                double[] data = new double[]{row, col, ps[0] < 200 || ps[3] < 1 ? 0 : 255, ps[3]};
                result.put(row, col, data);
            }
        }
        return result;
    }

    public Mat mask(String name) {
        Mat mask = maskMap.get(name);
        if (null != mask) return mask;
        if (name.startsWith("position:")) {
            mask = mask(name.replaceFirst("position:", ""));
            if (null == mask) return null;
            mask = position(mask);
        } else if (name.startsWith("file:")) {
            mask = cvService.imdecode(name, Imgcodecs.IMREAD_UNCHANGED);
        } else {
            URL resource = getClass().getClassLoader().getResource("static/images/" + name);
            if (null == resource) return null;
            String logo = resource.getFile();
            if (OSUtil.getCurrentOS().equals(OSUtil.OSType.Windows)) logo = logo.substring(1);
            mask = mask("file://" + logo);
        }
        maskMap.put(name, mask);
        return mask;
    }

    public void lingan(Mat full, Mat logo, double x, double y, SortedMap<Integer, Rect> wrapper) {
        Rect rect = new Rect((int) x + logo.width() / 2, (int) y + logo.height() / 2, logo.width(), logo.height());
        Mat roi = full.submat(rect);
        logo.copyTo(roi);
        if (null == wrapper) return;
        rect.x -= rect.width;
        rect.y -= rect.height;
        wrapper.put(rect.x, rect);
    }

    public Rect wrapper(Rect rect, Rect r) {
        Rect wrapper = new Rect(r.x - rect.x, r.y - rect.y, r.width, r.height);
        if (wrapper.x < 0) {
            wrapper.width += wrapper.x;
            wrapper.x = 0;
        }
        if (wrapper.y < 0) {
            wrapper.height += wrapper.y;
            wrapper.y = 0;
        }
        if (wrapper.x + wrapper.width > rect.width) wrapper.width = rect.width - wrapper.x;
        if (wrapper.y + wrapper.height > rect.height) wrapper.height = rect.height - wrapper.y;
        if (wrapper.width < 1 || wrapper.height < 1) return null;
        return wrapper;
    }

    public boolean lingan(Mat image, String logoName, SortedMap<Integer, Rect> wrapper) {
        Mat logo = mask(logoName);
        if (null == logo) return false;
        Point point = new Point(-108, 146); // 第一个水印相对图片右上角的中心偏移
        Point offset = new Point(-370, 100); // 第二个水印相对第一个水印的中心偏移
        Point top = new Point(-284, -226); // 上部水平线的中心偏移
        Point bottom = new Point(-134, 336); // 下部水平线的中心偏移
        double width = image.width(), height = image.height();
        Rect2d edge = new Rect2d(-logo.width() / 2, -logo.height() / 2, width + logo.width() / 2, height + logo.height() / 2);
        Mat full = new Mat(image.rows() + logo.rows() * 2, image.cols() + logo.cols() * 2, logo.type()); // 绘制区域
        // 中心直线
        for (double x = width + point.x, y = point.y; x > edge.x && y < edge.height; x += offset.x, y += offset.y) {
            lingan(full, logo, x, y, wrapper);
        }
        // 顶部平行线
        for (double parallelX = width + point.x, parallelY = point.y, toggle = 1; parallelX > edge.x; toggle *= -1) {
            if (toggle > 0) {
                parallelX += top.x;
                parallelY += top.y;
            } else {
                parallelX += bottom.x * toggle;
                parallelY += bottom.y * toggle;
            }
            for (double x = parallelX, y = parallelY; x > edge.x; x += offset.x, y += offset.y) {
                if (y < edge.y || y > edge.height) continue; // 不在可视区域内
                lingan(full, logo, x, y, wrapper);
            }
        }
        // 底部平行线
        for (double parallelX = width + point.x, parallelY = point.y, toggle = 1; parallelY < edge.height; toggle *= -1) {
            if (toggle > 0) {
                parallelX += bottom.x;
                parallelY += bottom.y;
            } else {
                parallelX += top.x * toggle;
                parallelY += top.y * toggle;
            }
            for (double x = parallelX, y = parallelY; y < edge.height; x += offset.x, y += offset.y) {
                if (x < edge.x || x > edge.width) continue; // 不在可视区域内
                lingan(full, logo, x, y, wrapper);
            }
        }
        full.submat(new Rect(logo.width(), logo.height(), image.width(), image.height())).copyTo(image);
        return true;
    }

}
