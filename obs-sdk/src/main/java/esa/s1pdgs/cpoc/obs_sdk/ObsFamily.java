package esa.s1pdgs.cpoc.obs_sdk;

/**
 * Available families of object in the OBS
 * 
 * @author Viveris Technologies
 */
public enum ObsFamily {
    AUXILIARY_FILE, EDRS_SESSION, L0_SLICE, L0_ACN, L1_SLICE, L1_ACN, L2_SLICE, L2_ACN, L0_SEGMENT, L0_BLANK, UNKNOWN,
    
    AUXILIARY_FILE_ZIP,
    L0_SEGMENT_ZIP, L0_BLANK_ZIP,
    L0_ACN_ZIP,L1_ACN_ZIP,L2_ACN_ZIP, 
    L0_SLICE_ZIP, L1_SLICE_ZIP, L2_SLICE_ZIP;
}
